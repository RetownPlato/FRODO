/*
FRODO: a FRamework for Open/Distributed Optimization
Copyright (C) 2008-2017  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek

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

/**
 * 
 */
package frodo2.algorithms.localSearch.mgm.mgm2;

import frodo2.communication.MessageWithPayload;

/**
 * @author Brammert Ottens, 1 apr. 2011
 * 
 */
public class NOGO extends MessageWithPayload<String> {

	/** Used for serialization */
	private static final long serialVersionUID = 3687443315093354999L;

	/** Default constructor used for externalization only */
	public NOGO () {
		super.type = MGM2.NO_GO_MSG_TYPE;
	}
	
	/**
	 * Constructor
	 * @param receiver the receiver of the message
	 */
	public NOGO(String receiver) {
		super(MGM2.NO_GO_MSG_TYPE, receiver);
	}
	
	/**
	 * @author Brammert Ottens, 11 apr. 2011
	 * @return the receiver of the message
	 */
	public String getReceiver() {
		return this.getPayload();
	}
}
