## 2024-05-24 - Started

## 2024-05-24 - O(N) Iteration in Cache Size Calculation
**Learning:** Calculating dynamic capacity dynamically via `store.values.sumOf { it.sizeBytes }` effectively incurs an O(N) cost on every `put` because it runs when `trimToCapacity()` calls `dynamicCapacity`. When cache sizes get large, this causes O(N²) overall insertion performance and UI stutters.
**Action:** Use an `AtomicLong` (O(1)) to track the running sum of cache size instead of dynamically iterating through the entire collection.

## 2024-05-25 - O(N^2) loop optimization
**Learning:** Checking for item existence using `list.none { it.id == post.id }` within loops processing remote data causes an O(N^2) operation, harming parse performance for large JSON blocks.
**Action:** Use an auxiliary `HashSet` to trace seen IDs (`seenIds.add(post.id)`) which reduces time complexity of deduplication check from O(N) to O(1) resulting in overall O(N) operation for loop.
