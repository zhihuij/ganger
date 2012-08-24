Ganger - A simple automate tool
===============

Ganger is a simple automate tool. It can automate deploy packages, launch
target processes, update packages deployment(kill the old process and launch
the new one), restart target processes and monitor the status of target 
process.

It saves your time for development, and make it easier to deploy your systems
on many target machines simultaneously.

 * Homepage: <http://github.com/netease/ganger>
 * Wiki: <https://github.com/netease/ganger/wiki/>
 * Issues: <https://github.com/netease/ganger/issues/>
 * Tags: java, automatation

Features
--------

**Automatation**: help you do some automate tasks, for example: deploy packages,
start processes, stop processes and update deployments etc.
(Note: deploy action *ONLY* support compressed binary executables(.tar.gz)
currently.)

**Lifecycle management**: you can start, stop, restart or update the target
process.

**Dependence management**: we organize the package as a directed graph with no
cycles, when a process of a package crashed or started, the processes of the 
package which depend on it will be notified. Automate action will operate on 
packages as the topological order of the dependence graph(or reverse order for 
actions similar to `stop`).

**Status monitor**: monitor the target process's status.

Getting Started
---------------
Clone this repo

    git clone git://github.com/netease/ganger.git

and run 

    ant release

this will create two files in directory target/release: auto-master-x.x.x.tar.gz
for master node, auto-slave-x.x.x.tar.gz for slave node.

Before doing the actual work, you need to setup a [ZooKeeper](http://zookeeper.apache.org/) 
instance, and use the address to start master and slave nodes.

Start ganger in three steps: 1) start the ZooKeeper instance; 2) start the slave
nodes by using `bin/auto-slave.sh`; 3) start the master node by using `bin/ganger.sh`.

Use ganger as following:

    load ../sample/xmpp.properties  # load a project configuration file
	deploy xmpp  # deploy the packages defined by this project 
	launch xmpp  # and lauch them

See [here](https://github.com/netease/ganger/blob/master/sample/xmpp.properties) 
for a sample configuration format for a project.

Document
--------
See user manual [here](https://github.com/netease/ganger/tree/master/doc)