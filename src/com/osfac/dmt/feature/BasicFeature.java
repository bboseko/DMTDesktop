package com.osfac.dmt.feature;

import java.io.Serializable;

/**
 * Default implementation of the Feature interface.
 */
public class BasicFeature extends AbstractBasicFeature implements Serializable {

    private static final long serialVersionUID = -7891137208054228529L;
    private Object[] attributes;
    private short modCount = 0;
    private boolean modified = false;

    /**
     * Constructs a BasicFeature with the given FeatureSchema specifying the
     * attribute names and types.
     */
    public BasicFeature(FeatureSchema featureSchema) {
        super(featureSchema);
        attributes = new Object[featureSchema.getAttributeCount()];
    }

    /**
     * A low-level accessor that is not normally used. It is called by
     * ViewSchemaPlugIn.
     */
    public void setAttributes(Object[] attributes) {
        Object[] attributesOld = this.attributes;
        this.attributes = attributes;
        if (attributes != null) {
            if (attributesOld.length != attributes.length) {
                modified = true;
            } else {
                for (int i = 0; i < attributes.length; i++) {
                    if (attributesOld[i] != null && attributesOld[i] != attributes[i]) {
                        modified = true;
                    }
                }
            }
        }
    }

    /**
     * Sets the specified attribute.
     *
     * @param attributeIndex the array index at which to put the new attribute
     * @param newAttribute the new attribute
     */
    public void setAttribute(int attributeIndex, Object newAttribute) {
        modCount++;
        if (attributes[attributeIndex] != null || modCount > attributes.length) {
            modified = true;
        }
        attributes[attributeIndex] = newAttribute;
    }

    /**
     * Returns the specified attribute.
     *
     * @param i the index of the attribute to get
     * @return the attribute
     */
    public Object getAttribute(int i) {
        return attributes[i];
        //We used to eat ArrayOutOfBoundsExceptions here. I've removed this behaviour
        //because ArrayOutOfBoundsExceptions are bugs and should be exposed. [Bob Boseko]
    }

    /**
     * A low-level accessor that is not normally used. It is called by
     * ViewSchemaPlugIn.
     */
    public Object[] getAttributes() {
        return attributes;
    }

    /**
     * @return true if any attribute of this Feature (including Geometry) has
     * been set more than once.
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * @param modified - allows the modified flag to be set or reset
     */
    public void setModified(boolean modified) {
        this.modified = modified;
        modCount = 0;
    }
}
