## 2024-05-24 - Started

## 2024-05-24 - O(N) Iteration in Cache Size Calculation
**Learning:** Calculating dynamic capacity dynamically via `store.values.sumOf { it.sizeBytes }` effectively incurs an O(N) cost on every `put` because it runs when `trimToCapacity()` calls `dynamicCapacity`. When cache sizes get large, this causes O(N²) overall insertion performance and UI stutters.
**Action:** Use an `AtomicLong` (O(1)) to track the running sum of cache size instead of dynamically iterating through the entire collection.
## 2024-10-06 - O(N^2) Lookup Optimization in List
**Learning:** Using `List.none { it.id == post.id }` within a loop creates an O(N^2) complexity because `none` iterates over the list which grows linearly.
**Action:** Replace `List.none` or `List.contains` checks on growing collections with an O(1) `Set.add` tracking mechanism when maintaining uniqueness.
