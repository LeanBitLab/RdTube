## 2024-05-24 - Started

## 2024-05-24 - O(N) Iteration in Cache Size Calculation
**Learning:** Calculating dynamic capacity dynamically via `store.values.sumOf { it.sizeBytes }` effectively incurs an O(N) cost on every `put` because it runs when `trimToCapacity()` calls `dynamicCapacity`. When cache sizes get large, this causes O(N²) overall insertion performance and UI stutters.
**Action:** Use an `AtomicLong` (O(1)) to track the running sum of cache size instead of dynamically iterating through the entire collection.
## 2024-05-18 - Optimized Duplicate Checks Using HashSet
**Learning:** Found several places where a list traversal (`list.none { it.id == post.id }`) was used inside a loop over a pagination list, resulting in O(N^2) complexity. Using a HashSet inside the loop enables O(1) duplicate checks and vastly improves execution speed.
**Action:** Before optimizing one specific place, search the file (and others, if applicable) for similar patterns to apply the optimization consistently. In Kotlin, use `HashSet.add()` which returns `true` if the item was added, condensing duplicate check and tracking into a single O(1) operation.
