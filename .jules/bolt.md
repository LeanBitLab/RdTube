## 2024-05-24 - Started

## 2024-05-24 - O(N) Iteration in Cache Size Calculation
**Learning:** Calculating dynamic capacity dynamically via `store.values.sumOf { it.sizeBytes }` effectively incurs an O(N) cost on every `put` because it runs when `trimToCapacity()` calls `dynamicCapacity`. When cache sizes get large, this causes O(N²) overall insertion performance and UI stutters.
**Action:** Use an `AtomicLong` (O(1)) to track the running sum of cache size instead of dynamically iterating through the entire collection.
## 2026-10-06 - O(N^2) Loop Optimization via Set
**Learning:** Checking for duplicates using `list.none { it.id == post.id }` inside a loop results in O(N^2) time complexity. Using a `Set` and `set.add()` reduces this to O(1) per iteration, vastly improving performance for large lists.
**Action:** Use a `Set` to track already added items when removing duplicates from a list inside a loop.
