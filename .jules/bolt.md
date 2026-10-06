## 2024-06-25 - Avoid Eager Collection Operations in Tight Loops
**Learning:** Using `store.filter { ... }.minByOrNull { ... }` inside a `while` loop for cache eviction creates temporary `LinkedHashMap` instances on every iteration, leading to excessive GC pressure and O(N) memory allocations per loop cycle.
**Action:** Replace functional pipelines that allocate intermediate collections with explicit, allocation-free `for` loops when iterating over Maps or Collections within performance-critical tight loops (like cache eviction).
