/*
FRODO: a FRamework for Open/Distributed Optimization
Copyright (C) 2008-2012  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek

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

/** Distributed meeting scheduling problem generator */
package frodo2.benchmarks.meetings;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import frodo2.solutionSpaces.AddableInteger;

/** Distributed meeting scheduling problem generator
 * @author Thomas Leaute
 */
public class MeetingScheduling {

	/** Creates a problem instance and saves it to a file
	 * @param args 	[-i] [-EAV] [-PEAV] [-EASV] [-infinity value] [-maxCost value] nbrAgents nbrMeetings nbrAgentsPerMeeting nbrSlots
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {

		// The GNU GPL copyright notice
		System.out.println("FRODO  Copyright (C) 2008-2012  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek\n" +
				"This program comes with ABSOLUTELY NO WARRANTY.\n" +
				"This is free software, and you are welcome to redistribute it\n" +
				"under certain conditions. \n");
		
		// Parse the inputs
		if (args.length < 4 || args.length > 12) {
			System.err.println("Required input parameters: [-i] [-EAV] [-PEAV] [-EASV] [-infinity value] [-maxCost value] nbrAgents nbrMeetings nbrAgentsPerMeeting nbrSlots\n" +
					"\t -i [optional]               outputs an intensional problem\n" +
					"\t -EAV [optional]             Events As (private) Variables\n" +
					"\t -PEAV [optional]            Private Events As (private) Variables\n" +
					"\t -EASV [optional]            Events As Shared Variables\n" +
					"\t -infinity value [optional]  the cost incurred by violating one constraint; can be set to `infinity' to use hard constraints (default)\n" +
					"\t -maxCost value [optional]   if provided, each attendee assigns a random cost in [0, value] to having any meeting at each time slot");
			return;
		}
		LinkedList<String> params = new LinkedList<String> (Arrays.asList(args));
		boolean intensional = false;
		boolean eav = false;
		boolean peav = false;
		boolean easv = false;
		AddableInteger infinity = AddableInteger.PlusInfinity.PLUS_INF;
		Integer maxCost = null;
		while (true) {
			String param = params.getFirst();
			
			if (param.equals("-i")) {
				intensional = true;
				params.removeFirst();
				
			} else if (param.equals("-EAV")) {
				eav = true;
				params.removeFirst();
				
			} else if (param.equals("-PEAV")) {
				peav = true;
				params.removeFirst();
				
			} else if (param.equals("-EASV")) {
				easv = true;
				params.removeFirst();
				
			} else if (param.equals("-infinity")) {
				params.removeFirst();
				infinity = infinity.fromString(params.removeFirst());
				
			} else if (param.equals("-maxCost")) {
				params.removeFirst();
				maxCost = Integer.parseInt(params.removeFirst());
				
			} else 
				break;
		}
		
		if (!peav && !eav && !easv) 
			peav = true;
		
		if (intensional && infinity != AddableInteger.PlusInfinity.PLUS_INF) {
			System.err.println("WARNING! Producing an extensional problem.");
			intensional = false;
		}
		
		int nbrAgents = Integer.parseInt(params.removeFirst());
		int nbrMeetings = Integer.parseInt(params.removeFirst());
		int nbrAgentsPerMeeting = Integer.parseInt(params.removeFirst());
		int nbrSlots = Integer.parseInt(params.removeFirst());
		
		if (nbrAgents < nbrAgentsPerMeeting) {
			System.err.println("Not enough agents (" + nbrAgents + ") to have " + nbrAgentsPerMeeting + " per meeting");
			System.exit(1);
		}
		
		// Generate a random problem instance
		MeetingScheduling instance = new MeetingScheduling (nbrAgents, nbrMeetings, nbrAgentsPerMeeting, nbrSlots, maxCost);
		
		// Create XCSP and write to a file
		if (peav) {
			Document doc = instance.toXCSP(intensional, Mode.PEAV, infinity);
			new XMLOutputter(Format.getPrettyFormat()).output(doc, new FileWriter ("meetingScheduling_PEAV.xml"));
			System.out.println("Wrote meetingScheduling_PEAV.xml");
		}
		if (eav) {
			Document doc = instance.toXCSP(intensional, Mode.EAV, infinity);
			new XMLOutputter(Format.getPrettyFormat()).output(doc, new FileWriter ("meetingScheduling_EAV.xml"));
			System.out.println("Wrote meetingScheduling_EAV.xml");
		}
		if (easv) {
			Document doc = instance.toXCSP(intensional, Mode.EASV, infinity);
			new XMLOutputter(Format.getPrettyFormat()).output(doc, new FileWriter ("meetingScheduling_EASV.xml"));
			System.out.println("Wrote meetingScheduling_EASV.xml");
		}
	}
	
	/** The mode of encoding */
	private enum Mode {
		/** Events As (private) Variables */
		EAV, 
		/** Private Events As (private) Variables */
		PEAV, 
		/** Events As Shared Variables */
		EASV
	}
	
	/** For each meeting, its list of attendees */
	private ArrayList< ArrayList<Integer> > attendees;

	/** For each participant, its list of meetings */
	private ArrayList< ArrayList<Integer> > meetings;
	
	/** The cost that each participant assigns to having a meeting in each time slot */
	private ArrayList< ArrayList<Integer> > preferences;
	
	/** Number of available time slots for each meeting */
	private final int nbrSlots;

	/** Constructor
	 * @param nbrAgents 			number of agents in the overall pool of agents
	 * @param nbrMeetings 			number of meetings
	 * @param nbrAgentsPerMeeting 	number of agents per meeting
	 * @param nbrSlots 				number of available time slots for each meeting
	 * @param maxCost 				if not \c null, each attendee assigns a random cost in [0, value] to having any meeting at each time slot
	 */
	public MeetingScheduling(final int nbrAgents, final int nbrMeetings, final int nbrAgentsPerMeeting, int nbrSlots, Integer maxCost) {
		this.nbrSlots = nbrSlots;
		
		// Create the pool of agents
		ArrayList<Integer> pool = new ArrayList<Integer> (nbrAgents);
		this.meetings = new ArrayList< ArrayList<Integer> > (nbrAgents);
		for (int i = 0; i < nbrAgents; i++) {
			pool.add(i);
			this.meetings.add(new ArrayList<Integer> (nbrMeetings));
		}
		
		// Create the agents' preferences
		if (maxCost != null) {
			maxCost++; // so that rnd(0, maxCost) is in [0, maxCost]
			
			this.preferences = new ArrayList< ArrayList<Integer> > (nbrAgents);
			Random rnd = new Random ();
			
			for (int i = 0; i < nbrAgents; i++) {
				ArrayList<Integer> pref = new ArrayList<Integer> (nbrSlots);
				for (int j = nbrSlots - 1; j >= 0; j--) 
					pref.add(rnd.nextInt(maxCost));
				this.preferences.add(pref);
			}
		}
		
		// Create the meetings
		this.attendees = new ArrayList< ArrayList<Integer> > (nbrMeetings);
		for (int i = 0; i < nbrMeetings; i++) {
			
			// Randomly choose participants
			ArrayList<Integer> participants = new ArrayList<Integer> (nbrAgentsPerMeeting);
			this.attendees.add(participants);
			LinkedList<Integer> pool2 = new LinkedList<Integer> (pool);
			Collections.shuffle(pool2);
			for (int j = 0; j < nbrAgentsPerMeeting; j++) {
				Integer agent = pool2.removeFirst();
				participants.add(agent);
				this.meetings.get(agent).add(i);
			}
		}
	}

	/** Creates an XCSP representation of the problem
	 * @param intensional 	whether to use intensional constraints
	 * @param mode 			the mode
	 * @param infinity 		the cost incurred by violating one constraint
	 * @return an XCSP Document
	 */
	private Document toXCSP(boolean intensional, final Mode mode, AddableInteger infinity) {
		
		Element root = new Element ("instance");
		
		root.setAttribute("noNamespaceSchemaLocation", "src/frodo2/algorithms/XCSPschema" + (intensional ? "JaCoP" : "") + ".xsd", 
				Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
		
		// <presentation>
		Element elmt = new Element ("presentation");
		root.addContent(elmt);
		elmt.setAttribute("name", "Meeting scheduling instance");
		elmt.setAttribute("format", "XCSP 2.1_FRODO");
		elmt.setAttribute("maximize", "false");
		
		// <agents>
		elmt = new Element ("agents");
		root.addContent(elmt);
		HashSet<Integer> agents = new HashSet<Integer> ();
		for (ArrayList<Integer> participants : this.attendees) 
			for (Integer agent : participants) 
				agents.add(agent);
		elmt.setAttribute("nbAgents", Integer.toString(agents.size()));
		for (Integer agent : agents) {
			Element subElmt = new Element ("agent");
			elmt.addContent(subElmt);
			subElmt.setAttribute("name", "a" + agent);
		}
		
		// <domains>
		elmt = new Element ("domains");
		root.addContent(elmt);
		elmt.setAttribute("nbDomains", "1");
		Element subElmt = new Element ("domain");
		elmt.addContent(subElmt);
		subElmt.setAttribute("name", "slots");
		subElmt.setAttribute("nbValues", Integer.toString(nbrSlots));
		subElmt.setText("1.." + Integer.toString(nbrSlots));
		
		// <variables>
		elmt = new Element ("variables");
		root.addContent(elmt);
		for (int i = 0 ; i < this.attendees.size(); i ++) {
			ArrayList<Integer> participants = this.attendees.get(i);
			
			subElmt = new Element ("variable");
			elmt.addContent(subElmt);
			String owner = "a" + participants.get(0);
			subElmt.setAttribute("name", "m" + i + (mode != Mode.PEAV ? "" : owner));
			subElmt.setAttribute("domain", "slots");
			if (mode != Mode.EASV) 
				subElmt.setAttribute("agent", owner);

			if (mode == Mode.PEAV) {
				for (int j = 1; j < participants.size(); j++) {
					
					subElmt = new Element ("variable");
					elmt.addContent(subElmt);
					owner = "a" + participants.get(j);
					subElmt.setAttribute("name", "m" + i + owner);
					subElmt.setAttribute("domain", "slots");
					subElmt.setAttribute("agent", owner);
				}
			}
		}
		elmt.setAttribute("nbVariables", Integer.toString(elmt.getContentSize()));
		
		// <predicates> or <relations>
		Element relElmt = new Element ("relations");
		
		if (this.preferences != null) { // each agent expresses preferences over the time slots
			
			// Loop over the agents
			for (int i = 0; i < this.meetings.size(); i++) {
				String agent = "a" + i;
				ArrayList<Integer> pref = this.preferences.get(i);
				
				elmt = new Element ("relation");
				relElmt.addContent(elmt);
				elmt.setAttribute("name", agent + "_pref");
				elmt.setAttribute("semantics", "soft");
				elmt.setAttribute("nbTuples", Integer.toString(nbrSlots));
				elmt.setAttribute("arity", "1");
				
				StringBuilder builder = new StringBuilder ();
				for (int j = 1; j < nbrSlots; j++) 
					builder.append(Integer.toString(pref.get(j-1)) + ":" + j + " | ");
				builder.append(Integer.toString(pref.get(nbrSlots-1)) + ":" + nbrSlots);
				elmt.setText(builder.toString());
			}
		}
		
		if (mode == Mode.PEAV && intensional) {
			elmt = new Element ("predicates");
			root.addContent(elmt);
			
			Element eq = new Element ("predicate");
			elmt.addContent(eq);
			eq.setAttribute("name", "EQ");
			
			Element params = new Element ("parameters");
			eq.addContent(params);
			params.setText("int X int Y");
			
			params = new Element("expression");
			eq.addContent(params);
			
			eq = new Element ("functional");
			params.addContent(eq);
			eq.setText("eq(X, Y)");
			
			elmt.setAttribute("nbPredicates", Integer.toString(elmt.getContentSize()));
			
		} else {
			
			if (mode != Mode.PEAV && this.preferences == null) { // we need trivial unary constraints for agents involved in a single meeting
				subElmt = new Element ("relation");
				relElmt.addContent(subElmt);
				subElmt.setAttribute("name", "trivial1");
				subElmt.setAttribute("semantics", "soft");
				subElmt.setAttribute("arity", "1");
				subElmt.setAttribute("nbTuples", "1");
				subElmt.setAttribute("defaultCost", "0");
				subElmt.addContent("0:1");
			}

			if (!intensional) { // we need binary NEQ and EQ relations
				
				subElmt = new Element ("relation");
				relElmt.addContent(subElmt);
				subElmt.setAttribute("name", "NEQ");
				subElmt.setAttribute("semantics", "soft");
				subElmt.setAttribute("arity", "2");
				subElmt.setAttribute("nbTuples", Integer.toString(nbrSlots));
				subElmt.setAttribute("defaultCost", "0");
				
				StringBuilder builder = new StringBuilder (infinity.toString() + ": 1 1");
				for (int i = 2; i <= nbrSlots; i++) 
					builder.append("|" + i + " " + i);
				subElmt.addContent(builder.toString());
			
				if (mode == Mode.PEAV) { // we also need the binary EQ relation
					
					subElmt = new Element ("relation");
					relElmt.addContent(subElmt);
					subElmt.setAttribute("name", "EQ");
					subElmt.setAttribute("semantics", "soft");
					subElmt.setAttribute("arity", "2");
					subElmt.setAttribute("nbTuples", Integer.toString(nbrSlots));
					subElmt.setAttribute("defaultCost", infinity.toString());
					
					builder = new StringBuilder ("0: 1 1");
					for (int i = 2; i <= nbrSlots; i++) 
						builder.append("|" + i + " " + i);
					subElmt.addContent(builder.toString());
				}
			}
		}
		if (relElmt.getContentSize() > 0) {
			relElmt.setAttribute("nbRelations", Integer.toString(relElmt.getContentSize()));
			root.addContent(relElmt);
		}
		
		// <constraints>
		elmt = new Element ("constraints");
		root.addContent(elmt);
		
		if (this.preferences != null) { // soft preference constraints
			
			// Loop over the agents
			for (int i = 0; i < this.meetings.size(); i++) {
				String agent = "a" + i;
				
				// Loop over the meetings this agent participates in
				for (Integer meeting : this.meetings.get(i)) {
					
					subElmt = new Element ("constraint");
					elmt.addContent(subElmt);
					subElmt.setAttribute("name", agent + "_pref_m" + meeting);
					subElmt.setAttribute("scope", "m" + meeting + (mode != Mode.PEAV ? "" : agent));
					subElmt.setAttribute("arity", "1");
					subElmt.setAttribute("reference", agent + "_pref");
					subElmt.setAttribute("agent", agent);
				}
			}
		}
		
		if (mode == Mode.PEAV) {
			// Binary equality constraints: loop over the meetings
			for (int i = 0; i < this.attendees.size(); i++) {
				ArrayList<Integer> participants = this.attendees.get(i);
				
				// Loop over the pairs of participants
				for (int j = 0; j < participants.size() - 1; j++) {
					String varJ = "m" + i + "a" + participants.get(j);
					
					for (int k = j + 1; k < participants.size(); k++) {
						String varK = "m" + i + "a" + participants.get(k);

						subElmt = new Element ("constraint");
						elmt.addContent(subElmt);
						subElmt.setAttribute("name", varJ + "_equals_" + varK);
						subElmt.setAttribute("arity", "2");
						subElmt.setAttribute("scope", varJ + " " + varK);
						subElmt.setAttribute("reference", "EQ");
						
						if (intensional) {
							Element params = new Element ("parameters");
							subElmt.addContent(params);
							params.setText(varJ + " " + varK);
						}
					}
				}
			}
		}
		
		// allDifferent constraints: loop over the agents
		for (Integer agent : agents) {
			ArrayList<Integer> myMeetings = this.meetings.get(agent);
			String agentName = "a" + agent;
				
			if (intensional) { // use one allDifferent constraint
					
				if (myMeetings.size() > 1 || this.preferences == null) {
					
					subElmt = new Element ("constraint");
					elmt.addContent(subElmt);
					subElmt.setAttribute("name", agentName + "_availability");
					subElmt.setAttribute("agent", agentName);
					subElmt.setAttribute("arity", Integer.toString(myMeetings.size()));

					String scope = "";
					for (Integer meeting : myMeetings) 
						scope += "m" + meeting + (mode != Mode.PEAV ? "" : agentName) + " ";
					subElmt.setAttribute("scope", scope);

					subElmt.setAttribute("reference", "global:allDifferent");

					Element params = new Element ("parameters");
					subElmt.addContent(params);
					params.setText("[ " + scope + "]");
				}
					
			} else {
					
				if (myMeetings.size() >1 || mode == Mode.PEAV) { // use binary extensional NEQ constraints

					// Loop over all pairs of meetings
					for (int i = myMeetings.size() - 1; i >= 1; i--) {
						String varI = "m" + myMeetings.get(i) + (mode != Mode.PEAV ? "" : agentName);
						
						for (int j = i - 1; j >= 0; j--) {
							String varJ = "m" + myMeetings.get(j) + (mode != Mode.PEAV ? "" : agentName);
							
							subElmt = new Element ("constraint");
							elmt.addContent(subElmt);
							subElmt.setAttribute("name", varI + "_neq_" + varJ + (mode != Mode.PEAV ? "_" + agentName : ""));
							subElmt.setAttribute("arity", "2");
							subElmt.setAttribute("scope", varI + " " + varJ);
							subElmt.setAttribute("reference", "NEQ");
							subElmt.setAttribute("agent", agentName);
						}
					}
					
				} else if (this.preferences == null) { // use unary always-satisfied constraint
					
					subElmt = new Element ("constraint");
					elmt.addContent(subElmt);
					subElmt.setAttribute("name", agentName + "_availability");
					subElmt.setAttribute("arity", "1");
					subElmt.setAttribute("scope", "m" + myMeetings.get(0));
					subElmt.setAttribute("reference", "trivial1");
					subElmt.setAttribute("agent", agentName);
				}
			}
		}
		
		elmt.setAttribute("nbConstraints", Integer.toString(elmt.getContentSize()));

		return new Document (root);
	}

}
