package org.luaj.vm2;



import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by nirenr on 2019/10/20.
 */

public class LuaList extends LuaTable implements Metatable {
    private static final LuaString N = valueOf("n");

    /**
     * the array values
     */
    private ArrayList<LuaValue> array = new ArrayList<>();

    /**
     * metatable for this table, or null
     */
    protected Metatable m_metatable;

    /**
     * Construct empty table
     */
    public LuaList() {
        array = new ArrayList<>();
    }

    /**
     * Construct table with preset capacity.
     *
     * @param narray capacity of array part
     */
    public LuaList(int narray) {
        array = new ArrayList<>(narray);
        presize(narray);
    }

    /**
     * Construct table of unnamed elements.
     *
     * @param varargs Unnamed elements in order {@code value-1, value-2, ... }
     */
    public LuaList(Varargs varargs) {
        this(varargs, 1);
    }

    /**
     * Construct table of unnamed elements.
     *
     * @param varargs  Unnamed elements in order {@code value-1, value-2, ... }
     * @param firstarg the index in varargs of the first argument to include in the table
     */
    public LuaList(Varargs varargs, int firstarg) {
        int nskip = firstarg - 1;
        int n = Math.max(varargs.narg() - nskip, 0);
        presize(n, 1);
        set(N, valueOf(n));
        for (int i = 1; i <= n; i++)
            set(i, varargs.arg(i + nskip));
    }

    public int type() {
        return LuaValue.TTABLE;
    }

    public String typename() {
        return "table";
    }

    public boolean istable() {
        return true;
    }

    public LuaTable checktable() {
        return this;
    }

    public LuaTable opttable(LuaTable defval) {
        return this;
    }

    public void presize(int narray) {
        /*for (int i = array.size(); i < narray; i++) {
            array.add(LuaValue.NIL);
        }*/
    }

    /**
     * Get the length of the array part of the table.
     *
     * @return length of the array part, does not relate to count of objects in the table.
     */
    protected int getArrayLength() {
        return array.size();
    }

    /**
     * Get the length of the hash part of the table.
     *
     * @return length of the hash part, does not relate to count of objects in the table.
     */
    protected int getHashLength() {
        return 0;
    }

    public LuaValue getmetatable() {
        return (m_metatable != null) ? m_metatable.toLuaValue() : null;
    }

    public LuaValue setmetatable(LuaValue metatable) {
        boolean hadWeakKeys = m_metatable != null && m_metatable.useWeakKeys();
        boolean hadWeakValues = m_metatable != null && m_metatable.useWeakValues();
        m_metatable = metatableOf(metatable);
        if ((hadWeakKeys != (m_metatable != null && m_metatable.useWeakKeys())) ||
                (hadWeakValues != (m_metatable != null && m_metatable.useWeakValues()))) {
            // force a rehash
        }
        return this;
    }

    public LuaValue get(int key) {
        LuaValue v = rawget(key);
        return v.isnil() && m_metatable != null ? gettable(this, valueOf(key)) : v;
    }

    public LuaValue get(LuaValue key) {
        LuaValue v = rawget(key);
        return v.isnil() && m_metatable != null ? gettable(this, key) : v;
    }

    public LuaValue rawget(int key) {
        if (key > 0 && key <= array.size()) {
            LuaValue[] arr = new LuaValue[array.size()];
            array.toArray(arr);
            LuaValue v = m_metatable == null ? array.get(key - 1) : m_metatable.arrayget(arr, key - 1);
            return v != null ? v : NIL;
        }
        return NIL;
    }

    public LuaValue rawget(LuaValue key) {
        if(key.eq_b(N)){
            return LuaInteger.valueOf(rawlen());
        }
        if (!key.isinttype())
            throw new LuaError("array key only integer");

        int ikey = key.toint();
        if (ikey > 0 && ikey <= array.size()) {
            LuaValue[] arr = new LuaValue[array.size()];
            array.toArray(arr);
            LuaValue v = m_metatable == null
                    ? array.get(ikey - 1) : m_metatable.arrayget(arr, ikey - 1);
            return v != null ? v : NIL;
        } else {
            return LuaValue.NIL;
        }
    }


    public void set(int key, LuaValue value) {
        if(mConst)
            throw new LuaError("can not be set a const table");
        if (m_metatable == null || !rawget(key).isnil() || !settable(this, LuaInteger.valueOf(key), value))
            rawset(key, value);
    }

    /**
     * caller must ensure key is not nil
     */
    public void set(LuaValue key, LuaValue value) {
        if(mConst)
            throw new LuaError("can not be set a const table");
        if (!key.isvalidkey() && !metatag(NEWINDEX).isfunction())
            typerror("table index");
        if (m_metatable == null || !rawget(key).isnil() || !settable(this, key, value))
            rawset(key, value);
    }

    public void rawset(int key, LuaValue value) {
        if(mConst)
            throw new LuaError("can not be set a const table");
        arrayset(key, value);
    }

    /**
     * caller must ensure key is not nil
     */
    public void rawset(LuaValue key, LuaValue value) {
        if(mConst)
            throw new LuaError("can not be set a const table");
        if(key.eq_b(N)){
            fullList(value.checkint());
            return;
        }
        if (!key.isinttype())
            throw new LuaError("array key only integer");
        arrayset(key.toint(), value);
    }

    /**
     * Set an array element
     */
    private boolean arrayset(int key, LuaValue value) {
        //fullList(key);
        if (key - 1 == array.size()) {
            array.add(value);
            return true;
        }
        if (key > 0 && key <= array.size()) {
            array.set(key - 1, value.isnil() ? null :
                    (m_metatable != null ? m_metatable.wrap(value) : value));
            return true;
        }
        throw new LuaError("array insert position out of bounds");
        //    return false;
    }

    private void fullList(int key) {
        for (int i = array.size(); i < key; i++) {
            array.add(LuaValue.NIL);
        }
        for (int i = array.size()-1; i >= key; i--) {
            array.remove(i);
        }
    }

    /**
     * Remove the element at a position in a list-table
     *
     * @param pos the position to remove
     * @return The removed item, or {@link #NONE} if not removed
     */
    public LuaValue remove(int pos) {
        int n = rawlen();
        if (pos == 0)
            pos = n;
        else if (pos > n)
            return NONE;
        LuaValue v = rawget(pos);
        array.remove(pos - 1);
        return v.isnil() ? NONE : v;
    }

    /**
     * Insert an element at a position in a list-table
     *
     * @param pos   the position to remove
     * @param value The value to insert
     */
    public void insert(int pos, LuaValue value) {
        if (pos == 0)
            pos = rawlen() + 1;
        if (pos == array.size() + 1)
            array.add(value);
        else if (pos > 0 && pos <= array.size())
            array.add(pos, value);
        else
            throw new LuaError("array insert position out of bounds");
    }

    /**
     * Concatenate the contents of a table efficiently, using {@link Buffer}
     *
     * @param sep {@link LuaString} separater to apply between elements
     * @param i   the first element index
     * @param j   the last element index, inclusive
     * @return {@link LuaString} value of the concatenation
     */
    public LuaValue concat(LuaString sep, int i, int j) {
        Buffer sb = new Buffer();
        if (i <= j) {
            sb.append(get(i).checkstring());
            while (++i <= j) {
                sb.append(sep);
                sb.append(get(i).checkstring());
            }
        }
        return sb.tostring();
    }

    public int length() {
        return m_metatable != null ? len().toint() : rawlen();
    }

    public LuaValue len() {
        final LuaValue h = metatag(LEN);
        if (h.toboolean())
            return h.call(this);
        return LuaInteger.valueOf(rawlen());
    }

    public int rawlen() {
        int a = getArrayLength();
        int n = a + 1, m = 0;
        while (n > m + 1) {
            int k = (n + m) / 2;
            if (!rawget(k).isnil())
                m = k;
            else
                n = k;
        }
        return m;
    }

    /**
     * Get the next element after a particular key in the table
     *
     * @return key, value or nil
     */
    public Varargs next(LuaValue key) {
        int i = 0;
        do {
            // find current key index
            if (!key.isnil()) {
                if (key.isinttype()) {
                    i = key.toint();
                    if (i > 0 && i <= array.size()) {
                        break;
                    }
                }
                i += 1 + array.size();
            }
        } while (false);

        LuaValue[] arr = new LuaValue[array.size()];
        array.toArray(arr);
        // check array part
        for (; i < array.size(); ++i) {
            if (array.get(i) != null) {
                LuaValue value = m_metatable == null ? array.get(i) : m_metatable.arrayget(arr, i);
                if (value != null) {
                    return varargsOf(LuaInteger.valueOf(i + 1), value);
                }
            }
        }

        // nothing found, push nil, return nil.
        return NIL;
    }

    /**
     * Get the next element after a particular key in the
     * contiguous array part of a table
     *
     * @return key, value or none
     */
    public Varargs inext(LuaValue key) {
        int k = key.checkint() + 1;
        LuaValue v = rawget(k);
        return v.isnil() ? NONE : varargsOf(LuaInteger.valueOf(k), v);
    }


    private void dropWeakArrayValues() {
        LuaValue[] arr = new LuaValue[array.size()];
        array.toArray(arr);
        for (int i = 0; i < array.size(); ++i) {
            m_metatable.arrayget(arr, i);
        }
    }

    private int countIntKeys(int[] nums) {
        int total = 0;
        int i = 1;

        // Count integer keys in array part
        for (int bit = 0; bit < 31; ++bit) {
            if (i > array.size())
                break;
            int j = Math.min(array.size(), 1 << bit);
            int c = 0;
            while (i <= j) {
                if (array.get(i++ - 1) != null)
                    c++;
            }
            nums[bit] = c;
            total += c;
        }
        return total;
    }


    // ----------------- sort support -----------------------------
    //
    // implemented heap sort from wikipedia
    //
    // Only sorts the contiguous array part.
    //

    /**
     * Sort the table using a comparator.
     *
     * @param comparator {@link LuaValue} to be called to compare elements.
     */
    public void sort(LuaValue comparator) {
        if (m_metatable != null && m_metatable.useWeakValues()) {
            dropWeakArrayValues();
        }
        int n = array.size();
        while (n > 0 && array.get(n - 1) == null)
            --n;
        if (n > 1)
            heapSort(n, comparator);
    }

    private void heapSort(int count, LuaValue cmpfunc) {
        heapify(count, cmpfunc);
        for (int end = count - 1; end > 0; ) {
            swap(end, 0);
            siftDown(0, --end, cmpfunc);
        }
    }

    private void heapify(int count, LuaValue cmpfunc) {
        for (int start = count / 2 - 1; start >= 0; --start)
            siftDown(start, count - 1, cmpfunc);
    }

    private void siftDown(int start, int end, LuaValue cmpfunc) {
        for (int root = start; root * 2 + 1 <= end; ) {
            int child = root * 2 + 1;
            if (child < end && compare(child, child + 1, cmpfunc))
                ++child;
            if (compare(root, child, cmpfunc)) {
                swap(root, child);
                root = child;
            } else
                return;
        }
    }

    private boolean compare(int i, int j, LuaValue cmpfunc) {
        LuaValue[] arr = new LuaValue[array.size()];
        array.toArray(arr);
        LuaValue a, b;
        if (m_metatable == null) {
            a = array.get(i);
            b = array.get(j);
        } else {
            a = m_metatable.arrayget(arr, i);
            b = m_metatable.arrayget(arr, j);
        }
        if (a == null || b == null)
            return false;
        if (!cmpfunc.isnil()) {
            return cmpfunc.call(a, b).toboolean();
        } else {
            return a.lt_b(b);
        }
    }

    private void swap(int i, int j) {
        LuaValue a = array.get(i);
        array.set(i, array.get(j));
        array.set(j, a);
    }

    /**
     * This may be deprecated in a future release.
     * It is recommended to count via iteration over next() instead
     *
     * @return count of keys in the table
     */
    public int keyCount() {
        LuaValue k = LuaValue.NIL;
        for (int i = 0; true; i++) {
            Varargs n = next(k);
            if ((k = n.arg1()).isnil())
                return i;
        }
    }

    /**
     * This may be deprecated in a future release.
     * It is recommended to use next() instead
     *
     * @return array of keys in the table
     */
    public LuaValue[] keys() {
        Vector l = new Vector();
        LuaValue k = LuaValue.NIL;
        while (true) {
            Varargs n = next(k);
            if ((k = n.arg1()).isnil())
                break;
            l.addElement(k);
        }
        LuaValue[] a = new LuaValue[l.size()];
        l.copyInto(a);
        return a;
    }

    // equality w/ metatable processing
    public LuaValue eq(LuaValue val) {
        return eq_b(val) ? TRUE : FALSE;
    }

    public boolean eq_b(LuaValue val) {
        if (this == val) return true;
        if (m_metatable == null || !val.istable()) return false;
        LuaValue valmt = val.getmetatable();
        return valmt != null && LuaValue.eqmtcall(this, m_metatable.toLuaValue(), val, valmt);
    }

    /**
     * Unpack all the elements of this table
     */
    public Varargs unpack() {
        return unpack(1, array.size());
    }

    /**
     * Unpack all the elements of this table from element i
     */
    public Varargs unpack(int i) {
        return unpack(i, array.size());
    }

    /**
     * Unpack the elements from i to j inclusive
     */
    public Varargs unpack(int i, int j) {
        int n = j + 1 - i;
        switch (n) {
            case 0:
                return NONE;
            case 1:
                return get(i);
            case 2:
                return varargsOf(get(i), get(i + 1));
            default:
                if (n < 0)
                    return NONE;
                LuaValue[] v = new LuaValue[n];
                while (--n >= 0)
                    v[n] = get(i + n);
                return varargsOf(v);
        }
    }


    private static final Slot[] NOBUCKETS = {};

    // Metatable operations

    public boolean useWeakKeys() {
        return false;
    }

    public boolean useWeakValues() {
        return false;
    }

    public LuaValue toLuaValue() {
        return this;
    }

    public LuaValue wrap(LuaValue value) {
        return value;
    }

    public LuaValue arrayget(LuaValue[] array, int index) {
        return array[index];
    }
}
