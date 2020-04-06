/*
FRODO: a FRamework for Open/Distributed Optimization
Copyright (C) 2008-2020  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek

FRODO is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

FRODO is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.


How to contact the authors: 
<https://frodo-ai.tech>
 */

package org.jacop.constraints;

import java.util.ArrayList;
import java.util.Arrays;

import org.jacop.core.FailException;
import org.jacop.core.IntVarCloneable;
import org.jacop.core.StoreCloneable;

/** A cloneable version of the ElementIntegerFast constraint
 * @author Thomas Leaute
 */
public class ElementIntegerFastCloneable extends ElementIntegerFast implements ConstraintCloneableInterface<ElementIntegerFastCloneable> {

    /**
     * It specifies indexOffset within an element constraint list[index - indexOffset] = value.
     */
    private final int indexOffset;

   /**
     * It constructs an element constraint. 
     * 
     * @param index variable index
     * @param list list of variables from which an index-th element is taken
     * @param value a value of the index-th element from list
     */
	public ElementIntegerFastCloneable(IntVarCloneable index, ArrayList<? extends Integer> list, IntVarCloneable value) {
		super(index, list, value);
		indexOffset = 0;
	}

    /**
     * It constructs an element constraint. 
     * 
     * @param index variable index
     * @param list list of variables from which an index-th element is taken
     * @param value a value of the index-th element from list
     */
	public ElementIntegerFastCloneable(IntVarCloneable index, int[] list, IntVarCloneable value) {
		super(index, list, value);
		indexOffset = 0;
	}

	/** Constructor
	 * @param index 		the index variable
	 * @param list 			the list of allowed values, indexed by the index variable
	 * @param value 		the variable whose value is constrained to be = list[index - indexOffset]
	 * @param indexOffset 	the index offset
	 */
	public ElementIntegerFastCloneable(IntVarCloneable index, int[] list, IntVarCloneable value, int indexOffset) {
		super(index, list, value, indexOffset);
		this.indexOffset = indexOffset;
	}

    /**
     * It constructs an element constraint. 
     * 
     * @param index variable index
     * @param list list of variables from which an index-th element is taken
     * @param value a value of the index-th element from list
     * @param indexOffset shift applied to index variable. 
     */
	public ElementIntegerFastCloneable(IntVarCloneable index, ArrayList<? extends Integer> list, IntVarCloneable value, int indexOffset) {
		super(index, list, value, indexOffset);
		this.indexOffset = indexOffset;
	}

	/** @see ConstraintCloneableInterface#cloneInto(StoreCloneable) */
	@Override
	public ElementIntegerFastCloneable cloneInto(StoreCloneable targetStore) 
	throws FailException {
		
		IntVarCloneable index2 = targetStore.findOrCloneInto((IntVarCloneable) this.index);
		if (index2.dom().isEmpty()) 
			throw StoreCloneable.failException;
		
		IntVarCloneable value2 = targetStore.findOrCloneInto((IntVarCloneable) this.value);
		if (value2.dom().isEmpty()) 
			throw StoreCloneable.failException;
		
		return new ElementIntegerFastCloneable (index2, Arrays.copyOf(this.list, this.list.length), value2, this.indexOffset);
	}
}
