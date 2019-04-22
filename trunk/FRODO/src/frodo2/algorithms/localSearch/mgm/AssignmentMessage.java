/*
FRODO: a FRamework for Open/Distributed Optimization
Copyright (C) 2008-2019  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek

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

package frodo2.algorithms.localSearch.mgm;

import frodo2.communication.MessageWith2Payloads;
import frodo2.solutionSpaces.Addable;

/** A message holding an assignment to a variable
 * @param <V> type used for variable values
 */
public class AssignmentMessage <V extends Addable<V>> extends MessageWith2Payloads<String, V> {

	/** Empty constructor used for externalization */
	public AssignmentMessage () { }

	/** Constructor 
	 * @param var 		the variable
	 * @param val 		the value assigned to the variable \a var
	 */
	public AssignmentMessage (String var, V val) {
		super (MGM.OUTPUT_MSG_TYPE, var, val);
	}

	/** @return the variable */
	public String getVariable () {
		return this.getPayload1();
	}

	/** @return the value */
	public V getValue () {
		return this.getPayload2();
	}

}
