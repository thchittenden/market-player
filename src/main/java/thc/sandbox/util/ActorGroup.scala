package thc.sandbox.util

import collection._

import akka.actor.Actor
import akka.actor.ActorRef

object ActorGroup {
	def apply(as: ActorRef*) = { new ActorGroup() ++= as }
}

class ActorGroup extends mutable.HashSet[ActorRef] {
			
	def !(message: Any) {
		this foreach (_ ! message)
	}
	
}