package defectDistribution;

public class HashPairs <K, N> implements  Comparable <HashPairs<K,N>>{
	private K key;
	private N value;
	
	public HashPairs (K k, N v) {
		key = k;
		value = v;
	}
	
	public N getPairValue (){
		return this.value;
	}
	
	public K getPairkey() {
		return this.key;
	}

	@Override
	public int compareTo(HashPairs<K,N> hPair) {
		Number val1 = (Number) this.getPairValue();
		Number val2 = (Number) hPair.getPairValue();
		if (val1.intValue() < val2.intValue()) return 1;
		if (val1.intValue() > val2.intValue()) return -1;
		return 0;
	}
	
}
