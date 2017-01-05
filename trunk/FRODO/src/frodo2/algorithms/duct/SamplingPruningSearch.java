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

package frodo2.algorithms.duct;

import java.util.List;

import org.jdom2.Element;

import frodo2.algorithms.AgentInterface;
import frodo2.algorithms.duct.samplingMethods.SamplingProcedure;
import frodo2.algorithms.duct.termination.TerminationCondition;
import frodo2.algorithms.duct.BoundStatsMsg;
import frodo2.algorithms.duct.COSTmsg;
import frodo2.algorithms.duct.Normalize;
import frodo2.algorithms.duct.OUTmsg;
import frodo2.algorithms.duct.Sampling;
import frodo2.algorithms.duct.SamplingPruning;
import frodo2.algorithms.duct.SearchNode;
import frodo2.algorithms.duct.SearchNodePruning;
import frodo2.algorithms.duct.VALUEmsg;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration.DFSview;
import frodo2.communication.Message;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.AddableReal;
import frodo2.solutionSpaces.DCOPProblemInterface;
import frodo2.solutionSpaces.UtilitySolutionSpace;

/**
 * @author Brammert Ottens, 7 jul. 2011
 * 
 * @param <V> type used for domain values
 */
public class SamplingPruningSearch <V extends Addable<V>> extends SamplingPruning <V> {
	
	/**
	 * Constructor for the stats reporter
	 * 
	 * @param parameters	listeners parameters (not used for the moment)
	 * @param problem		problem description
	 */
	public SamplingPruningSearch(Element parameters, DCOPProblemInterface<V, AddableReal> problem) {
		super(parameters, problem);
	}
	
	/**
	 * Constructor
	 * 
	 * @param problem		problem description
	 * @param parameters	listeners parameters (not used for the moment)
	 */
	public SamplingPruningSearch(DCOPProblemInterface<V, AddableReal> problem, Element parameters) {
		super(problem, parameters);
	}
	
	/** 
	 * @see frodo2.communication.IncomingMsgPolicyInterface#notifyIn(frodo2.communication.Message)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void notifyIn(Message msg) {
		String type = msg.getType();
		
		if (type.equals(ASS_MSG_TYPE)) {
			AssignmentMessage<V> msgCast = (AssignmentMessage<V>)msg;
			assignment.put(msgCast.getSender(), msgCast.getValue());
			if(this.reportStats) {
				System.out.println("Variable " + msgCast.getSender() + " = " + msgCast.getValue());

				// When we have received all messages, print out the corresponding utility. 
				/// @author Thomas Leaute
				if (this.assignment.keySet().containsAll(this.problem.getVariables())) 
					System.out.println("Total "
							+ (this.problem.maximize() ? "utility: " : "cost: ")
							+ this.problem.getUtility(this.assignment, true).getUtility(0));
			}
			return;
		} else if (type.equals(BOUND_MSG_TYPE)) {
			BoundStatsMsg msgCast = (BoundStatsMsg)msg;
			finalBound = finalBound == null ? msgCast.getFinalBound() : finalBound.add(msgCast.getFinalBound());
			return;
		}
		
		if (!started)
			init();
		
		if (type.equals(VALUE_MSG_TYPE)) {
			VALUEmsg<V> msgCast = (VALUEmsg<V>)msg;
			VariableInfo varInfo = (VariableInfo)infos.get(msgCast.getReceiver());
			
			if(!varInfo.receivedNormalize) {
				varInfo.toBeProcessed.add(msgCast);
			} else {
				// update context
				if(varInfo.setContext(msgCast.getSender(), msgCast.getVariables(), msgCast.getValues())) {

					if(varInfo.leaf) {
						queue.sendMessage(owners.get(varInfo.parent), new COSTmsg(varInfo.parent, varInfo.solveLeaf()));
					} else {
						// sample the domain of the variable
						if(varInfo.sample())
							// report the value
							reportValue(varInfo, false);
						else
							queue.sendMessage(owners.get(varInfo.parent), new COSTmsg(varInfo.parent, varInfo.penalty));
					}
				} else {
					queue.sendMessage(owners.get(varInfo.parent), new COSTmsg(varInfo.parent, null));
				}
			}
		}
		
		if (type.equals(VALUE_FIN_MSG_TYPE)) {
			VALUEmsg<V> msgCast = (VALUEmsg<V>)msg;
			VariableInfo varInfo = (VariableInfo)infos.get(msgCast.getReceiver());

			// update context
			varInfo.parentFinished = true;
			
			varInfo.setContext(msgCast.getSender(), msgCast.getVariables(), msgCast.getValues());
			
			boolean finished = varInfo.leaf || varInfo.finishedSampling(error, delta);
			
			if(finished) {
				if(varInfo.leaf) {
					varInfo.solveLeaf();
					if (this.reportStats) 
						queue.sendMessage(AgentInterface.STATS_MONITOR, varInfo.getAssignmentMessage(varInfo.currentValue));
				} else {
					reportValue(varInfo, finished);
					if (this.reportStats) 
						queue.sendMessage(AgentInterface.STATS_MONITOR, varInfo.getAssignmentMessage(varInfo.currentValue));
				}
				if(--this.numberOfActiveVariables == 0)
					queue.sendMessageToSelf(new Message(AgentInterface.AGENT_FINISHED));
			} else {
				// sample the domain of the variable
				varInfo.sample();

				// report the value
				reportValue(varInfo, finished);
			}
		}
		
		else if (type.equals(COST_MSG_TYPE)) {
			COSTmsg msgCast = (COSTmsg)msg;
			VariableInfo varInfo = (VariableInfo)infos.get(msgCast.getReceiver());
			
			if(varInfo.storeCOSTmsg(msgCast)) { // all cost messages have been received
				if(varInfo.parentFinished) { // the parent has finished sampling, and found its optimal value
					boolean finished = varInfo.finishedSampling(error, delta);
					
					if(!finished) // if not converged, perform sampling
						varInfo.sample();
					else { // report optimal value to the stats reporter
						if (this.reportStats) {
							queue.sendMessage(AgentInterface.STATS_MONITOR, varInfo.getAssignmentMessage(varInfo.variableID, varInfo.currentValue));
							if(varInfo.parent == null) // the root has finished
								queue.sendMessage(AgentInterface.STATS_MONITOR, new BoundStatsMsg(varInfo.getFinalBound()));
						}
						if(--this.numberOfActiveVariables == 0)
							queue.sendMessageToSelf(new Message(AgentInterface.AGENT_FINISHED));
					}
					
					// report the new value to the children
					reportValue(varInfo, finished);
				} else {
					queue.sendMessage(owners.get(varInfo.getParent()), varInfo.getCostMessage());
				}
			} else if (varInfo.infeasiblePath) {
				// sample the domain of the variable
				varInfo.sample();

				// report the value
				reportValue(varInfo, false);
			}
			
		}
		
		else if (type.equals(Normalize.OUT_MSG_TYPE)) {
			OUTmsg<V> msgCast = (OUTmsg<V>)msg;
			String receiver = msgCast.getVariable();
			VariableInfo varInfo = (VariableInfo)infos.get(receiver);
			assert varInfo != null;
			varInfo.setSize(msgCast.getSize());
			varInfo.setSeparator(msgCast.getSeparators());
			scalingFactor = msgCast.getScalingFactor();
			varInfo.penalty = scalingFactor == null ? null : penalty.divide(scalingFactor);
			varInfo.receivedNormalize = true;
			for(UtilitySolutionSpace<V, AddableReal> space : msgCast.getSpaces())
				varInfo.storeConstraint(space);
					
			if(varInfo.parent == null) {
				if(varInfo.leaf) {
					if (this.reportStats) 
						queue.sendMessage(AgentInterface.STATS_MONITOR, varInfo.getAssignmentMessage(receiver, varInfo.solveSingleton(maximize)));

					if(--this.numberOfActiveVariables == 0) {
						queue.sendMessageToSelf(new Message(AgentInterface.AGENT_FINISHED));
					}
				} else {
					varInfo.setContext(receiver, new String[0], (V[])new Addable[0]);
					varInfo.sample();
					reportValue(varInfo, false);
				}
			} else if (!varInfo.toBeProcessed.isEmpty()) {
				assert varInfo.toBeProcessed.size() == 1;
				VALUEmsg<V> msgV = varInfo.toBeProcessed.get(0);
				// update context
				varInfo.setContext(msgV.getSender(), msgV.getVariables(), msgV.getValues());

				if(varInfo.leaf) {
					queue.sendMessage(owners.get(varInfo.parent), new COSTmsg(varInfo.parent, varInfo.solveLeaf()));
				} else {
					// sample the domain of the variable
					varInfo.sample();

					// report the value
					reportValue(varInfo, false);
				}
			}
			
		}
		
		else if (type.equals(DFSgeneration.OUTPUT_MSG_TYPE)) {
			DFSgeneration.MessageDFSoutput<V, AddableReal> msgCast = (DFSgeneration.MessageDFSoutput<V, AddableReal>) msg;

			String var = msgCast.getVar();
			DFSview<V, AddableReal> neighbours = msgCast.getNeighbors();
			
			// get the lower neighbours
			List<String> children = neighbours.getChildren();
			
			// get the parent
			String parent = neighbours.getParent();
			
			infos.put(var, newVariableInfo(var, problem.getDomain(var), parent, children));
		}

	}
	
	/** @see frodo2.algorithms.duct.Sampling#newVariableInfo(java.lang.String, V[], java.lang.String, java.util.List) */
	@Override
	protected Sampling<V>.VariableInfo newVariableInfo(String variableID, V[] domain, String parent, List<String> children) {
		return new VariableInfo(variableID, domain, parent, children, samplingClass, terminationClass);
	}
	
	/**
	 * Convenience class to hold variable information
	 * 
	 * @author Brammert Ottens, 7 jul. 2011
	 *
	 */
	protected class VariableInfo extends SamplingPruning<V>.VariableInfo {
		
		/** Whether the path is infeasible */
		protected boolean infeasiblePath = false;
		
		/**
		 * Constructor
		 * 
		 * @param variableID		the name of the variable
		 * @param domain			the domain of the variable
		 * @param parent			the parent of the variable
		 * @param children			list of the children of the variable
		 * @param samplingClass		the class of the sampling method
		 * @param terminationClass 	the class of the termination condition
		 */
		public VariableInfo(String variableID, V[] domain, String parent, List<String> children, Class <SamplingProcedure<V>> samplingClass, Class<TerminationCondition<V>> terminationClass) {
			super(variableID, domain, parent, children, samplingClass, terminationClass);
		}
		
		/** @see frodo2.algorithms.duct.Sampling.VariableInfo#createNode(int, boolean, boolean) */
		@Override
		protected SearchNode<V> createNode(int domainSize, boolean maximize, boolean IGNORE_INF) {
			return new SearchNodePruning<V>(domainSize, maximize, IGNORE_INF);
		}
		
		/** @see frodo2.algorithms.duct.Sampling.VariableInfo#sample() */
		@Override
		public boolean sample() {
			this.infeasiblePath = false;
			return super.sample();
		}
		
		
		/**
		 * 
		 * @author Brammert Ottens, 7 jul. 2011
		 * @param msg the message to be stored
		 * @return	\c true when enough cost messages have been received, \c false otherwise
		 */
		@Override
		public boolean storeCOSTmsg(COSTmsg msg) {

			AddableReal cost = msg.getCost();
			
			if( cost == null)
				this.reportedSample = infeasibleUtility;
				
			this.reportedSample = reportedSample == null ? msg.getCost() : reportedSample.add(msg.getCost());
			
			// store the COST message
			
			if(++this.costMessagesReceived == nbrChildren) {
				cost = node.storeCost(this.currentValueIndex, reportedSample, infeasibleUtility, maximize);
				if(!node.random)
					sampler.processSample(node, infeasibleUtility, maximize);
				this.reportedSample = null;
				this.costMessagesReceived = 0;
				if(node.feasible)
					nextCostMsg = new COSTmsg(parent, cost == null ? penalty : cost);
				else
					nextCostMsg = new COSTmsg(parent, null);
				if(reportedSample == infeasibleUtility) {
					this.infeasiblePath = true;
					return false;
				}
					
				return true;
			}
			
			return false;
		}
	}
	
}
