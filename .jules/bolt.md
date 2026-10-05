## 2024-05-24 - Started

## 2024-05-24 - O(N) Iteration in Cache Size Calculation
**Learning:** Calculating dynamic capacity dynamically via `store.values.sumOf { it.sizeBytes }` effectively incurs an O(N) cost on every `put` because it runs when `trimToCapacity()` calls `dynamicCapacity`. When cache sizes get large, this causes O(N²) overall insertion performance and UI stutters.
**Action:** Use an `AtomicLong` (O(1)) to track the running sum of cache size instead of dynamically iterating through the entire collection.
## 2023-10-05 - [Compose LazyList Performance]
**Learning:** Using `indexOf(item)` inside Compose `items()` loops in a `LazyVerticalGrid` or `LazyColumn` causes unnecessary O(N) operations during recomposition, which leads to dropped frames and laggy scrolling, especially for large lists.
**Action:** Always use `itemsIndexed()` when the index is needed inside the `item` block to provide an O(1) index lookup and ensure smooth scrolling performance.
