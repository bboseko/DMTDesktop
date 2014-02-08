package com.osfac.dmt.mail;

import com.osfac.dmt.I18N;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Security;
import java.util.Properties;
import java.util.logging.Level;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

public class MailSender {

    final private static String CHARSET = "charset=ISO-8859-1";
    final private static String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
    final private static int DEFAULT_SMTP_PORT = 25;
    final private Session _session;

    // Constructeur ne1: Connexion au serveur mail
    public MailSender(final String host, final int port, final String userName, final String password, final boolean ssl) {
        final String strPort = String.valueOf(port);
        final Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", strPort);
        if (ssl) {
            // Connection SSL
            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            props.put("mail.smtp.socketFactory.class", SSL_FACTORY);
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.socketFactory.port", strPort);
        }
        if (null == userName || null == password) {
            _session = Session.getDefaultInstance(props, null);
        } else {
            // Connexion avec authentification
            _session = Session.getDefaultInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password);
                }
            });
        }
    }

    // Autres constructeurs
    public MailSender(final String host, final String userName, final String password, final boolean ssl) {
        this(host, DEFAULT_SMTP_PORT, userName, password, ssl);
    }

    public MailSender(final String host, final String userName, final String password) {
        this(host, DEFAULT_SMTP_PORT, userName, password, false);
    }

    public MailSender(final String host, final int port) {
        this(host, port, null, null, false);
    }

    public MailSender(final String host) {
        this(host, DEFAULT_SMTP_PORT, null, null, false);
    }

    // Convertit un texte au format html en texte brut
    private static String HtmlToText(final String s) {
        final HTMLEditorKit kit = new HTMLEditorKit();
        final Document doc = kit.createDefaultDocument();
        try {
            kit.read(new StringReader(s), doc, 0);
            return doc.getText(0, doc.getLength()).trim();
        } catch (final IOException | BadLocationException ioe) {
            return s;
        }
    }

    // Defini les fichiers e joindre
    private void setAttachmentPart(final String[] attachmentPaths, final MimeMultipart related, final MimeMultipart attachment,
            final String body, final boolean htmlText)
            throws MessagingException {
        for (int i = 0; i < attachmentPaths.length; ++i) {
            // Creation du fichier e inclure
            final MimeBodyPart messageFilePart = new MimeBodyPart();
            final DataSource source = new FileDataSource(attachmentPaths[i]);
            final String fileName = source.getName();
            messageFilePart.setDataHandler(new DataHandler(source));
            messageFilePart.setFileName(fileName);
            // Image e inclure dans un texte au format HTML ou piece jointe
            if (htmlText && null != body && body.matches(
                    ".*<img[^>]*src=[\"|']?cid:([\"|']?" + fileName + "[\"|']?)[^>]*>.*")) {
                // " <-- pour eviter une coloration syntaxique desastreuse...
                messageFilePart.setDisposition("inline");
                messageFilePart.setHeader("Content-ID", '<' + fileName + '>');
                related.addBodyPart(messageFilePart);
            } else {
                messageFilePart.setDisposition("attachment");
                attachment.addBodyPart(messageFilePart);
            }
        }
    }

    // Texte alternatif = texte + texte html
    private void setHtmlText(final MimeMultipart related, final MimeMultipart alternative, final String body)
            throws MessagingException {
        // Plain text
        final BodyPart plainText = new MimeBodyPart();
        plainText.setContent(HtmlToText(body), "text/plain; " + CHARSET);
        alternative.addBodyPart(plainText);
        // Html text ou Html text + images incluses
        final BodyPart htmlText = new MimeBodyPart();
        htmlText.setContent(body, "text/html; " + CHARSET);
        if (0 != related.getCount()) {
            related.addBodyPart(htmlText, 0);
            final MimeBodyPart tmp = new MimeBodyPart();
            tmp.setContent(related);
            alternative.addBodyPart(tmp);
        } else {
            alternative.addBodyPart(htmlText);
        }
    }

    // Definition du corps de l'e-mail
    private void setContent(final Message message, final MimeMultipart alternative, final MimeMultipart attachment,
            final String body)
            throws MessagingException {
        if (0 != attachment.getCount()) {
            // Contenu mixte: Pieces jointes +  texte
            if (0 != alternative.getCount() || null != body) {
                // Texte alternatif = texte + texte html
                final MimeBodyPart tmp = new MimeBodyPart();
                tmp.setContent(alternative);
                attachment.addBodyPart(tmp, 0);
            } else {
                // Juste du texte
                final BodyPart plainText = new MimeBodyPart();
                plainText.setContent(body, "text/plain; " + CHARSET);
                attachment.addBodyPart(plainText, 0);
            }
            message.setContent(attachment);
        } else {
            // Juste un message texte
            if (0 != alternative.getCount()) {
                // Texte alternatif = texte + texte html
                message.setContent(alternative);
            } else {
                // Texte
                message.setText(body);
            }
        }
    }

    // Prototype ne1: Envoi de message avec piece jointe
    public void sendMessage(final MailMessage mailMsg) throws MessagingException {
        final Message message = new MimeMessage(_session);
        // Subect
        message.setSubject(mailMsg.getSubject());
        // Expediteur
        message.setFrom(mailMsg.getFrom());
        // Destinataires
        message.setRecipients(Message.RecipientType.TO, mailMsg.getTo());
        message.setRecipients(Message.RecipientType.CC, mailMsg.getCc());
        message.setRecipients(Message.RecipientType.BCC, mailMsg.getBcc());
        // Contenu + pieces jointes + images
        final MimeMultipart related = new MimeMultipart("related");
        final MimeMultipart attachment = new MimeMultipart("mixed");
        final MimeMultipart alternative = new MimeMultipart("alternative");
        final String[] attachments = mailMsg.getAttachmentURL();
        final String body = (String) mailMsg.getContent();
        final boolean html = mailMsg.isHtml();
        if (null != attachments) {
            setAttachmentPart(attachments, related, attachment, body, html);
        }
        if (html && null != body) {
            setHtmlText(related, alternative, body);
        }
        setContent(message, alternative, attachment, body);
        // Date d'envoi
        message.setSentDate(mailMsg.getSendDate());
        // Envoi
        Transport.send(message);
        // Reinitialise le message
        mailMsg.reset();
    }

    // Exemples
    public static void Main(final String[] args) {
        try {
            // connexion au serveur de mail
//        final MailSender mail1 = new MailSender("smtp.xxxxxx.xxx");
//        // Message simple : (from et to sont indispensables)
            final MailMessage msg = new MailMessage();
//        msg.setFrom("xxxxxx@xxxxxx.xxx");
//        msg.setTo("xxxxxx@xxxxxx.xxx");
//        msg.setCc("xxxxxx@xxxxxx.xxx");
//        msg.setSubject("sujet");
//        msg.setContent("corps du message", false);
//        mail1.sendMessage(msg);


            // connexion e un autre serveur de mail
            // (l'activation du compte pop est necessaire pour gmail)
            final MailSender mail2 = new MailSender("smtp.gmail.com", 465,
                    "dmt@osfac.net", "osfaclab01", true);

            // Message avec texte html + images incluses + pieces jointes
            msg.setFrom(new InternetAddress("dmt@osfac.net", "Martin john"));
            msg.setTo("bobnoblesse@gmail.com");
//        msg.setCc(
//                new InternetAddress[]{
//                    new InternetAddress("gaston@gmail.net", "Gaston lagaffe"),
//                    new InternetAddress("gilbert@gmail.net")
//                });
            msg.setSubject("sujet");
            msg.setContent("<p><h1>Salut</h1></p>"
                    + "<p>Image 1:<img src=\"cid:image1.jpg\"></p>"
                    + "<p>Image 2:<img src=\"cid:image2.jpg\"></p>"
                    + "<p>Encore image 1:<img src=\"cid:image1.jpg\"></p>", true);
//        msg.setAttachmentURL(new String[]{"c:\\toto.txt", "c:\\image1.jpg",
//                    "c:\\tata.txt", "c:\\image2.jpg"
//                });
            mail2.sendMessage(msg);
        } catch (UnsupportedEncodingException | MessagingException ex) {
            JXErrorPane.showDialog(null, new ErrorInfo(I18N.get("com.osfac.dmt.Config.Error"), ex.getMessage(), null, null, ex, Level.SEVERE, null));
        }
    }
}