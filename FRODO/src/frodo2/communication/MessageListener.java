/*
FRODO: a FRamework for Open/Distributed Optimization
Copyright (C) 2008-2015  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek

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
<http://frodo2.sourceforge.net/>
*/

package frodo2.communication;

import java.util.Collection;

/** A general interface for modules listening for messages
 * @author Thomas Leaute
 * @param <T> the class used for message types
 */
public interface MessageListener <T> {

	/** Sets the queue from which the sendMessage() should be called when the message must be forwarded
	 * @param queue the queue
	 */
	public void setQueue(Queue queue);
	
	/** @return the message types this listener wants to listen to */
	public Collection <T> getMsgTypes ();
	
}
