# Distributed-System
# MP1
Write a program that allows you to **query distributed log files on multiple machines**, from any one of those machines. The scenario is that you have N (>5) machines, each generating a log file (named machine.i.log). You open a terminal on any of those N machines. You should now be able to execute a grep command that runs on all the log files across all machines, and produces
output on your terminal (with the appropriate lines to designate which log entries come from).

# MP2 Membership List Implementation  
This service maintains, at each machine in the system (at a daemon process), a list of the other machines that are connected and up.  
This membership list needs to be updated whenever:  
1. A machine (or its daemon) **joins** the group;  
2. A machine (or its daemon) **voluntarily leaves** the group;  
3. A machine (or its daemon) **crashes** from the group (you may assume that the machine does not recover for a long enough time).    
  
There is only one group at any point of time. Since we’re implementing the **crash/fail-stop model**, when a machine rejoins, it must do so   with an **id that includes a timestamp** - this distinguishes successive incarnations of the same machine (these are the ids held in the   membership lists). Notice that the id also has to contain the IP address.    

A machine **failure** must be reflected in at **least one membership lists within 2 seconds** (assuming synchronized clocks) – this is called **time-bounded completeness**, and it must be provided no matter what the network latencies are. A machine failure, join or leave must be reflected within **6 seconds at all membership lists**, assuming small network latencies. You’re told that at most four machines can fail simultaneously, and after four back-to-back failures, the next set of failure(s) don’t happen for at least 20 seconds. Your system must ensure completeness for all such failures (up to four simultaneous failures).      
  
Your algorithm must be **scalable to large numbers of machines**. For your experiments however, you may assume that you have N > 5 machines in the group at any given time. Note that this is not a limit on the set of machines eligible to be group members. Typical runs will involve about 7-10 VMs.    
  
You must use the heartbeating style of failure detection (not ping-ack like SWIM), **implemented in an extended ring**. In an extended ring, each process is located at a point on a **virtual ring** (you can choose the position in any way you like), and each process heartbeats to/from its immediate successor(s) and predecessor(s). Think of the “topology” (graph) you create among the processes for the failure detection, i.e., think of how many successors and predecessors you need to satisfy the requirements. DO NOT relay heartbeats, just send heartbeats directly to the monitors. Use the minimum number of monitors (heartbeat targets) to achieve the MP specification. 
   
Your algorithm must use a small bandwidth (messages per second) needed to meet the above requirements. So, for instance, don’t use all to  all pinging (it’s an overkill). For the failure detection or leaves, you cannot use a master or leader, since its failure must be detected as well. However, to enable machines to join the group, you can have a fixed contact machine that all potential members know about (the **“introducer”**). When the contact is down, no new members can join the group until the contact has rejoined – but failures should still be detected, and leaves allowed.  



