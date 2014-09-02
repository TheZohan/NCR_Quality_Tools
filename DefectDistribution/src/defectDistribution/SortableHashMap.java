package defectDistribution;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

@SuppressWarnings("serial")
public class SortableHashMap<K, N> extends HashMap<K, N> {
	@SuppressWarnings("unchecked")
	@Override
	public N put(K key, N value) {
		Number val =  (Number) get (key);
		if (containsKey (key)) {
			val = val.intValue()+1; 
			super.put(key, (N) val);
		}
		else super.put (key, value);	
		return get(key);
	}
	
	
	public ArrayList <HashPairs<K,N>> sortMapArray () {
		ArrayList <HashPairs<K,N>> pairs = new ArrayList <HashPairs<K,N>> ();  
		Iterator<java.util.Map.Entry<K, N>> itr = this.entrySet().iterator();
		while (itr.hasNext()) {
			java.util.Map.Entry<K, N> element = itr.next();
		  	K strVal = element.getKey();
		  	N intVal = element.getValue();
		  	HashPairs<K,N> pairElement = new HashPairs<K,N> (strVal, intVal); 
		  	pairs.add(pairElement);
		}
		
		Collections.sort(pairs);
		return pairs;
		
	}

}

