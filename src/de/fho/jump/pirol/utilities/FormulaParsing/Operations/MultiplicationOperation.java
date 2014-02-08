/*
 * Created on 23.06.2005 for PIROL
 *
 * SVN header information:
 *  $Author: michaudm $
 *  $Rev: 1559 $
 *  $Date: 2008-10-06 00:54:14 +0200 (lun., 06 oct. 2008) $
 *  $Id: MultiplicationOperation.java 1559 2008-10-05 22:54:14Z michaudm $
 */
package de.fho.jump.pirol.utilities.FormulaParsing.Operations;

import com.osfac.dmt.feature.Feature;

import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;

/**
 * Class to handle multiplications within a formula.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 1559 $
 * 
 */
public class MultiplicationOperation extends GenericOperation {
    
    /**
     * Sets the value, that will be operated on.
     */
    public MultiplicationOperation(FormulaValue value1, FormulaValue value2) {
        super(value1, value2);
        this.opString = "*";
    }
    
    /**
     * Returns the multiplied values of the sub-values or sub-operations of this operation
     *@param feature
     *@return multiplied values of the sub-values or sub-operations
     */
    public double getValue(Feature feature) {
        return this.value1.getValue(feature) * this.value2.getValue(feature);
    }

}
