# List immutability exercises — R1

Working file for the board atom **R1 · List.of vs List.copyOf trap**
(Craft: *Java Track — Board*, DOING).

**R1 exit:** test committed, red before green — a record whose compact
constructor fixes the trap with `List.copyOf`, asserting both
`UnsupportedOperationException` on mutation *and* independence from the
source list.

Only Part A counts towards that exit. Part B is parked material that is
outside the atom; do not pull it while R1 is in DOING (WIP = 1).

Collection tests go into `src/test/java/dev/lab/mechanics/ListFactoryTest.java`,
in the style already present there: JUnit 5 + AssertJ, with comments stating
the relationship each assertion demonstrates. Baseline is Java 25.

```bash
./gradlew test --tests '*ListFactoryTest'
```

Predict the outcome *before* running. End every session with a resume note
(`STATE` / `NEXT` / `OPEN`) under the atom in Craft.

---

## Part A — R1

### Done

| # | Test | Shows |
|---|------|-------|
| 1 | `listOf_given_a_single_list_argument` | `List.of(list)` has size 1: it is a `List<List<T>>`, not a copy. |
| 2 | `listOf_given_a_single_list_argument_aliases_the_source` | Outer list is frozen (UOE); the inner element `isSameAs` the source and is still mutable. |
| 3 | `unmodifiableList_is_a_view_not_a_copy` | Source mutations show through the view; `List.copyOf(view)` is a real copy. |
| 4 | `copyOf_is_independent_of_the_source_and_rejects_mutation` | The copy does not follow `source.add`; `copied.add` throws UOE. |

### Step 1 — Finish test 4

The resume note asked for `add`, `set` and `remove` to throw; the committed
test asserts only `add`.

**Expectations to add:**
- `copied.set(0, ...)` throws `UnsupportedOperationException`.
- `copied.remove(0)` throws `UnsupportedOperationException`.
- A comment on each assertion block naming the relationship (copy vs. sealed).

### Step 2 — Test 5: `copyOf_rejects_null_elements`

**Relationship:** `ArrayList` accepts `null` elements. The immutable
factories do not — they throw immediately. The difference is invisible at
compile time and surfaces at runtime.

**Setup:** an `ArrayList<String>` containing a single `null` element.

**Expectations:**
- The `ArrayList` itself accepted the `null` (AssertJ has a one-liner for
  "contains null").
- `List.copyOf` of that list throws `NullPointerException`.
- `List.of("a", null)` throws `NullPointerException`.

**Ask yourself before running:** is "no nulls" the contract you want for a
domain type that stores its items this way? Step 4 makes that decision for
`BetSlip`.

### Step 3 — `BetSlip`, red

**Relationship:** a record is only shallowly immutable. Its component
reference is final, but a `List` component still points at whatever list
the caller passed in — the same aliasing as test 2.

**Setup:**
- A record `BetSlip` with a single `List` component `items`, and **no**
  compact constructor.
- A new test class next to `ListFactoryTest` (e.g. `BetSlipTest`).
- Build a slip from a mutable `ArrayList`.

**Expectations (write them as the final, desired behavior):**
- After `source.add(...)`, `slip.items()` is unchanged (independence).
- `slip.items().add(...)` throws `UnsupportedOperationException`.

**Predict before running:** which of the two assertions fails first, and
with what message?

**Exit of this step:** the test fails for the predicted reason. Commit it
red.

### Step 4 — `BetSlip`, green

**Fix:** add a compact constructor that takes a defensive copy with
`List.copyOf`.

**Traps:**
- In a compact constructor you reassign the *parameter*
  (`items = List.copyOf(items);`). `this.items = ...` does not compile.
- `Collections.unmodifiableList(items)` would make the second assertion
  pass and leave the first one red — it is a view, not a copy (test 3).
- `List.copyOf` throws NPE on a `null` list and on `null` elements
  (test 5). That is now part of `BetSlip`'s contract.

**Exit of this step = R1 exit:** same test, now green. Commit.

### Step 5 — Close the atom

- Replace the resume note in Craft; `OPEN` is answered by the brief
  (JLS §15.12.2: variable-arity overloads are considered only if the strict
  and loose phases find nothing, so `List.of(E)` wins for any single
  non-array argument).
- Tick R1, pull R2 from the top of NEXT.

---

## Part B — Parked (outside R1)

Related list mechanics that R1 does not ask for. Candidates for future
atoms; each would need its own exit criterion on the board.

### B1 — `copyOf` does not copy what is already immutable

**Relationship:** `List.copyOf` returns the same instance if the argument is
already one of the JDK's immutable lists. An `unmodifiableList` view does
not qualify, because its backing list can still change.

**Expectations:**
- `copyOf` of `List.of("a", "b")` returns the **same instance** (`isSameAs`).
- `copyOf` of an `unmodifiableList` view returns a **different instance**
  (already asserted in test 3).

### B2 — `Arrays.asList`: fixed size, writable slots

**Relationship:** `Arrays.asList` wraps the array itself. The list cannot
grow or shrink, but its slots are writable, and writes flow in both
directions between the array and the list.

**Setup:** a `String[]` with two elements, wrapped via `Arrays.asList`.

**Expectations:**
- `add` on the list throws `UnsupportedOperationException`.
- `set(0, ...)` on the list succeeds and is visible in `array[0]`.
- Writing `array[1] = ...` is visible through the list.

### B3 — `subList` is a live window

**Relationship:** `subList` is a view over a range of the backing list.
Writes through either side are visible on the other, and structural changes
to the backing list invalidate the window.

**Setup:** an `ArrayList` of `"a", "b", "c"`; take `subList(0, 2)`.

**Expectations:**
- `source.set(0, "z")` is visible through the window.
- After `source.add(...)`, merely reading the window (e.g. `getFirst()`)
  throws `ConcurrentModificationException`.
- `subList` of a `List.of(...)` result throws
  `UnsupportedOperationException` on `add`.

### B4 — `reversed()`: a view where positions flip

**Relationship:** `List.reversed()` (JDK 21) is a live view where index `i`
maps to `size - 1 - i` of the source.

**Setup:** an `ArrayList` of `"a", "b", "c"`; call `reversed()` on it.

**Expectations, in this order:**
- The view reads `"c", "b", "a"`.
- After `source.add("d")`, the same view reads `"d", "c", "b", "a"`.
- `reversed.add("x")` leaves the source reading `"x", "a", "b", "c", "d"`.

### B5 — Equality ignores implementation

**Relationship:** the `List` interface defines equality as same elements in
the same order, regardless of implementation. `hashCode` follows the same
contract.

**Setup:** `List.of("a", "b")`, a fresh `ArrayList` with the same elements,
and an `unmodifiableList` view over that `ArrayList`.

**Expectations:**
- All three are mutually `isEqualTo` each other.
- The immutable and the mutable list have the same `hashCode`.
- Put the immutable list into a `HashMap` as a key, look it up with the
  mutable list — you find the entry.

### Further topics

- `Collectors.toUnmodifiableList()` vs `Stream.toList()` vs
  `Collectors.toList()`.
- `toArray()` — when it aliases the backing array and when it copies.
- `iterator.remove()` as a write path through restricted views.
