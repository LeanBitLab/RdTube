## 2024-05-24 - Started

## 2024-05-24 - O(N) Iteration in Cache Size Calculation
**Learning:** Calculating dynamic capacity dynamically via `store.values.sumOf { it.sizeBytes }` effectively incurs an O(N) cost on every `put` because it runs when `trimToCapacity()` calls `dynamicCapacity`. When cache sizes get large, this causes O(N²) overall insertion performance and UI stutters.
**Action:** Use an `AtomicLong` (O(1)) to track the running sum of cache size instead of dynamically iterating through the entire collection.

## 2024-06-03 - Eager Collection Operations in Tight Loops
**Learning:** In Kotlin, using eager collection operations like `Map.filter` inside tight loops (like a `while` loop for cache eviction) allocates temporary collections on each iteration. This causes heavy garbage collection pressure, leading to UI stutters and poor performance.
**Action:** Avoid eager collection operations like `filter` or `map` in performance-critical code or tight loops. Prefer allocation-free explicit `for` loops for these scenarios to minimize temporary object allocations.
