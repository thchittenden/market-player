package thc.sandbox.marketmanager.data

import com.lmax.disruptor.EventFactory

object Container extends EventFactory[Container] {
	def newInstance(): Container = new Container
}

class Container {
	var id: Option[Int] = None
	var value: Any = null;
}