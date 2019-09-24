# Distributed-File-System-Development
Co-Contributor @YicongY https://github.com/YicongY
# MP1
This program allows you to **query distributed log files on multiple machines**, from any one of those machines. The scenario is that you have N (>5) machines, each generating a log file (named machine.i.log). You open a terminal on any of those N machines. You should now be able to execute a grep command that runs on all the log files across all machines, and produces output on your terminal (with the appropriate lines to designate which log entries come from).

# MP2 Membership List Implementation  
We design our system that each node in the system will pick 4 neighbors (2 successors/2 predecessors) to send **heartbeat signals** to ensure  completeness up to 4 failures. To make sure that a failure must be detected within 2 seconds, we set the timeout equals to the upper limit: 2 seconds. Moreover, to compromise between bandwidth and false positive rate given the timeout, we set the heartbeat period to be 1 second.

Each node will maintain a **membership list** locally, where Node ID contains the information of timestamp and node’s IP address, Heartbeat Counter is used to decide the priority between different messages (the local membership list will be updated accordingly only if the node receives a message whose heartbeat counter is larger than the value stored in the local membership list), and Local Time is used for failure detection.  

**Details**  
This service maintains, at each machine in the system (at a daemon process), a list of the other machines that are connected and up.  
This membership list needs to be updated whenever:  
1. A machine (or its daemon) **joins** the group;  
2. A machine (or its daemon) **voluntarily leaves** the group;  
3. A machine (or its daemon) **crashes** from the group (you may assume that the machine does not recover for a long enough time).    
  
There is only one group at any point of time. Since we’re implementing the **crash/fail-stop model**, when a machine rejoins, it must do so with an **id that includes a timestamp** - this distinguishes successive incarnations of the same machine (these are the ids held in the membership lists). Notice that the id also has to contain the IP address.    

A machine **failure** must be reflected in at **least one membership lists within 2 seconds** (assuming synchronized clocks) – this is called **time-bounded completeness**, and it must be provided no matter what the network latencies are. A machine failure, join or leave must be reflected within **6 seconds at all membership lists**, assuming small network latencies. Assume that at most four machines can fail simultaneously, and after four back-to-back failures, the next set of failure(s) don’t happen for at least 20 seconds. The system must ensure completeness for all such failures (up to four simultaneous failures).      
  
The algorithm is **scalable to large numbers of machines**. For the experiments however, we assume that we have N > 5 machines in the group at any given time. Note that this is not a limit on the set of machines eligible to be group members. Typical runs will involve about 7-10 VMs.    
  
We use the heartbeating style of failure detection (not ping-ack like SWIM), **implemented in an extended ring**. In an extended ring, each process is located at a point on a **virtual ring**, and each process heartbeats to/from its immediate successor(s) and predecessor(s).  
   
The algorithm uses a small bandwidth (messages per second) needed to meet the above requirements. So, for instance, we don’t use all to all pinging (it’s an overkill). For the failure detection or leaves, we do not use a master or leader, since its failure must be detected as well. However, to enable machines to join the group, we have a fixed contact machine that all potential members know about (the **“introducer”**). When the contact is down, no new members can join the group until the contact has rejoined – but failures should still be detected, and leaves allowed.  

# MP3 Simple Distributed File System
**Coordinator (Master Server) Election & File Storage**  
  
We use per-key coordinator to make sure totally order and make our system scalable. For totally order, we further implement a queue on the coordinator to receive the put file request from clients. The coordinator will ask clients to send files according to queue which will satisfy totally order. We adopt a **Cassandra** like algorithm. First, to decide the position of a node in the **virtual ring**, we pass the ID of each node into a **consistent hash function**, then we mod the returned hash value by 2^8 . Second, each node has the full membership list (use MP2 to maintain the full membership list). To decide which coordinator we need to contact when we want to do file operations regarding to sdfsfilename, we do the same thing to sdfsfilename to decide its value in the ring, then the first node (we call it targetNode) whose value is equal or larger to the value of this file is the coordinator. To store a file in the SDFS, the procedure is the similar.  
  
**Replication Strategy**

Since the SDFS is required to be **tolerant up to 2 machine failures**, we store 3 replicas for each file to prevent file loss, and we simply replicate the file to the first 2 successors of targetNode.  
  
To deal with re-replication, first we maintain a neighbor list containing the first 2 predecessors and first 2 successors for each node. By doing this, files on a node might need to be moved or delete only when the neighbor list of this node is changed. We summarize our strategy as follows:

**1. Node Added:**
(1) The newly-added node will ask its first successor to check its SDFS files: for each file, once the new node is the targetNode corresponding to the file, the first successor will send it to the new node.  
  
(2) For all existing nodes whose predecessors are changed, they will check the SDFS files stored on them: for each file stored on a node, if the corresponding targetNode is no longer the first two predecessors of the node, we delete the file on the node.  
  
**2. Node Removed:**
  
If the successors of a node are changed, we check all the SDFS files stored on this node: for each file sdfsfilename, if the node is the new targetNode corresponding to the file, we send the file to its new first two successors.  

**Quorum Read/Write**
  
To write a file sdfsfilename, the client will send request to the corresponding coordinator, and the coordinator will replicate the file to its first 2 successors, once 2 of the 3 (quorum condition) writing processes are done, the coordinator will inform the client that the writing process is finished. At the same time, the coordinator will still try to write the file to the last replica.
  
To read a file sdfsfilename, based on our design, the coordinator will always keep the newest version of sdfsfilename by caching, then the coordinator could directly return the file back to the client, which is fast.

**Write-Write Conflict Detection**

For detecting write-write conflict regarding to sdfsfilename, the client will first send a message to the coordinator to ask it check the timestamp of sdfsfilename. If the last update time of the file is within 1 minute, the coordinator will ask the client to confirm the write.


