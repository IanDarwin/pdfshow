package net.rejmi.pdfshow;

import java.util.ArrayList;
import java.util.List;

public final class ObservableHelper {
	private List<Observer> list = new ArrayList<Observer>();
	void register(Observer obs) {
		list.add(obs);
	}
	void unregister(Observer obs) {
		list.remove(obs);
	}
	void tellThem(Object o) {
		list.forEach(obs->obs.letMeKnow(o));
	}
}
