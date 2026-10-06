## 2024-05-24 - Started

## 2024-05-24 - O(N) Iteration in Cache Size Calculation
**Learning:** Calculating dynamic capacity dynamically via `store.values.sumOf { it.sizeBytes }` effectively incurs an O(N) cost on every `put` because it runs when `trimToCapacity()` calls `dynamicCapacity`. When cache sizes get large, this causes O(N²) overall insertion performance and UI stutters.
**Action:** Use an `AtomicLong` (O(1)) to track the running sum of cache size instead of dynamically iterating through the entire collection.
## 2024-05-24 - O(N^2) Loop Optimization for ID Deduplication
**Learning:** Repeatedly iterating over a growing list to check for duplicates (`list.none { it.id == post.id }`) inside a loop over fetched items leads to an O(N^2) complexity, significantly increasing processing time as the list grows, especially for large datasets.
**Action:** Use a `HashSet` to store and check seen identifiers to achieve O(1) lookups per element, reducing the overall complexity to O(N).
