import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * A simple implementation of hash tables.
 *
 * @author Samuel A. Rebelsky
 * @author Your Name Here
 */
public class ChainedHashTable<K, V> implements HashTable<K, V> {

  // +-------+-----------------------------------------------------------
  // | Notes |
  // +-------+

  /*
   * Our hash table is stored as an array of ArrayLists of key/value pairs. Because of the design of
   * Java arrays, we declare that as type Object[] rather than ArrayList<Pair<K,V>>[] and cast
   * whenever we extract an an element. (SamR needs to find a better way to deal with this issue;
   * using ArrayLists doesn't seem like the best idea.)
   * 
   * We use chaining to handle collisions. (Well, we *will* use chaining once the table is
   * finished.)
   * 
   * We expand the hash table when the load factor is greater than LOAD_FACTOR (see constants
   * below).
   * 
   * Since some combinations of data and hash function may lead to a situation in which we get a
   * surprising relationship between values (e.g., all the hash values are 0 mod 32), when expanding
   * the hash table, we incorporate a random number. (Is this likely to make a big difference? Who
   * knows. But it's likely to be fun to explore.)
   * 
   * For experimentation and such, we allow the client to supply a Reporter that is used to report
   * behind-the-scenes work, such as calls to expand the table.
   * 
   * Bugs to squash.
   * 
   * [ ] Doesn't check for repeated keys in `set`.
   * 
   * [ ] Doesn't look for matching key in `get`.
   * 
   * [ ] Doesn't handle collisions.
   * 
   * Other features to add.
   * 
   * [ ] The `expand` method is not completely implemented.
   * 
   * [ ] The `remove` method is not implemented.
   * 
   * [ ] The `containsKey` method is not implemented.
   * 
   * [ ] An iterator.
   */

  // +-----------+-------------------------------------------------------
  // | Constants |
  // +-----------+

  /**
   * The load factor for expanding the table.
   */
  static final double LOAD_FACTOR = 0.5;

  // +--------+----------------------------------------------------------
  // | Fields |
  // +--------+

  /**
   * The number of values currently stored in the hash table. We use this to determine when to
   * expand the hash table.
   */
  int size = 0;

  /**
   * The array that we use to store the ArrayList of key/value pairs. (We use an array, rather than
   * an ArrayList, because we want to control expansion and ArrayLists of ArrayLists are just
   * weird.)
   */
  Object[] buckets;

  /**
   * An optional reporter to let us observe what the hash table is doing.
   */
  Reporter reporter;

  /**
   * Do we report basic calls?
   */
  boolean REPORT_BASIC_CALLS = false;

  /**
   * Our helpful random number generator, used primarily when expanding the size of the table..
   */
  Random rand;

  // +--------------+----------------------------------------------------
  // | Constructors |
  // +--------------+

  /**
   * Create a new hash table.
   */
  public ChainedHashTable() {
    this.rand = new Random();
    this.clear();
    this.reporter = null;
  } // ChainedHashTable

  /**
   * Create a new hash table that reports activities using a reporter.
   */
  public ChainedHashTable(Reporter reporter) {
    this();
    this.reporter = reporter;
  } // ChainedHashTable(Reporter)

  // +-------------------+-------------------------------------------
  // | SimpleMap methods |
  // +-------------------+

  /**
   * Determine if the hash table contains a particular key.
   */
  @Override
  public boolean containsKey(K key) {
    // STUB/HACK
    try {
      get(key);
      return true;
    } catch (Exception e) {
      return false;
    } // try/catch
  } // containsKey(K)

  /**
   * Apply a function to each key/value pair.
   */
  public void forEach(BiConsumer<? super K, ? super V> action) {
    for (Pair<K, V> pair : this) {
      action.accept(pair.key(), pair.value());
    } // for
  } // forEach(BiConsumer)

  /**
   * Get the value for a particular key.
   */
  @Override
  public V get(K key) {
    int index = find(key);
    @SuppressWarnings("unchecked")
    ArrayList<Pair<K, V>> alist = (ArrayList<Pair<K, V>>) buckets[index];
    if (alist == null) {
      if (REPORT_BASIC_CALLS && (reporter != null)) {
        reporter.report("get(" + key + ") failed");
      } // if reporter != null
      throw new IndexOutOfBoundsException("Invalid key: " + key);
    } else {
      Pair<K, V> pair = alist.get(0);
      if (pair.key().equals(key)) {
        if (REPORT_BASIC_CALLS && (reporter != null)) {
          reporter.report("get(" + key + ") => " + pair.value());
        }

        // if reporter != null
        return pair.value();
      } else {

        for (Pair<K, V> pairTemp : alist) {
          if (pairTemp.key().equals(key)) {
            return pairTemp.value();
          }
        }

        throw new IndexOutOfBoundsException("");
      }
    }
  } // get
  // get(K)

  /**
   * Iterate the keys in some order.
   */
  public Iterator<K> keys() {
    return MiscUtils.transform(this.iterator(), (pair) -> pair.key());
  } // keys()

  /**
   * Remove a key/value pair.
   */
  @Override
  public V remove(K key) {
    // STUB
    return null;
  } // remove(K)

  /**
   * Set a value.
   */
  @SuppressWarnings("unchecked")
  public V set(K key, V value) {
    V result = null;
    // If there are too many entries, expand the table.
    if (this.size > (this.buckets.length * LOAD_FACTOR)) {
      expand();
    } // if there are too many entries

    // Find out where the key belongs and put the pair there.
    int index = find(key);
    ArrayList<Pair<K, V>> alist = (ArrayList<Pair<K, V>>) this.buckets[index];
    // Special case: Nothing there yet
    if (alist == null) {
      alist = new ArrayList<Pair<K, V>>();
      this.buckets[index] = alist;
    }

    boolean found = false;

    for (Pair<K, V> pair : alist) {
      if (pair.key().equals(key)) {
        pair.setValue(value);
        found = true;
      }
    }

    if (!found) {
      alist.add(new Pair<K, V>(key, value));
      ++this.size;
    }


    // Report activity, if appropriate
    if (REPORT_BASIC_CALLS && (reporter != null)) {
      reporter.report("adding '" + key + ":" + value + "' to bucket " + index);
    } // if reporter != null

    // And we're done
    return result;
  } // set(K,V)

  /**
   * Get the size of the dictionary - the number of values stored.
   */
  @Override
  public int size() {
    return this.size;
  } // size()

  /**
   * Iterate the values in some order.
   */
  public Iterator<V> values() {
    return MiscUtils.transform(this.iterator(), (pair) -> pair.value());
  } // values()

  // +------------------+--------------------------------------------
  // | Iterator methods |
  // +------------------+

  /**
   * Iterate the key/value pairs in some order.
   */
  public Iterator<Pair<K, V>> iterator() {
    return new Iterator<Pair<K, V>>() {
      public boolean hasNext() {
        // STUB
        return false;
      } // hasNext()

      public Pair<K, V> next() {
        // STUB
        return null;
      } // next()
    }; // new Iterator
  } // iterator()

  // +-------------------+-------------------------------------------
  // | HashTable methods |
  // +-------------------+

  /**
   * Clear the whole table.
   */
  @Override
  public void clear() {
    this.buckets = new Object[41];
    this.size = 0;
  } // clear()

  /**
   * Dump the hash table.
   */
  @Override
  public void dump(PrintWriter pen) {
    pen.println("Capacity: " + this.buckets.length + ", Size: " + this.size);
    for (int i = 0; i < this.buckets.length; i++) {
      @SuppressWarnings("unchecked")
      ArrayList<Pair<K, V>> alist = (ArrayList<Pair<K, V>>) this.buckets[i];
      if (alist != null) {
        for (Pair<K, V> pair : alist) {
          pen.println("  " + i + ": <" + pair.key() + "(" + pair.key().hashCode() + "):"
              + pair.value() + ">");
        } // for each pair in the bucket
      } // if the current bucket is not null
    } // for each bucket
  } // dump(PrintWriter)

  // +------+------------------------------------------------------------
  // | Misc |
  // +------+

  /**
   * Should we report basic calls? Intended mostly for tracing.
   */
  public void reportBasicCalls(boolean report) {
    REPORT_BASIC_CALLS = report;
  } // reportBasicCalls

  // +---------+---------------------------------------------------------
  // | Helpers |
  // +---------+

  /**
   * Expand the size of the table.
   */
  void expand() {
    // Figure out the size of the new table
    int newSize = 2 * this.buckets.length + rand.nextInt(10);
    if (REPORT_BASIC_CALLS && (reporter != null)) {
      reporter.report("Expanding to " + newSize + " elements.");
    } // if reporter != null
    // STUB
  } // expand()

  /**
   * Find the index of the entry with a given key. If there is no such entry, return the index of an
   * entry we can use to store that key.
   */
  int find(K key) {
    return Math.abs(key.hashCode()) % this.buckets.length;
  } // find(K)

} // class ChainedHashTable<K,V>

