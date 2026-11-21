package com.playwise.graph

/**
 * Teaching content for the graph lessons and duels. Pure data: no platform or Compose APIs,
 * so it can be tested and reused (e.g. for a printable/PDF export later).
 *
 * Walkthroughs use small fixed example graphs, not the random boards, so every step can be
 * checked by hand. Keep the traces consistent with the pseudocode if either is edited.
 */
data class LearningResource(val title: String, val url: String, val note: String)

data class QuizItem(val question: String, val answer: String)

data class TeachingGuide(
    /** One-line takeaway shown at the top of the guide. */
    val summary: String,
    val intuition: List<String>,
    /** Vocabulary: term to definition. */
    val terms: List<Pair<String, String>>,
    val exampleTitle: String,
    val exampleSteps: List<String>,
    val pseudocode: String,
    val complexity: List<String>,
    val whyItWorks: String,
    val pitfalls: List<String>,
    val quiz: List<QuizItem>,
    val resources: List<LearningResource>,
    /** Where the real Kotlin implementation lives in this module. */
    val inThisApp: String,
)

private val clrs = LearningResource(
    "Cormen, Leiserson, Rivest, Stein: Introduction to Algorithms (4th ed., MIT Press, 2022)", "",
    "Part VI, Graph Algorithms. Chapter 20 covers BFS/DFS/SCC; chapter 22 covers single-source shortest paths."
)
private val erickson = LearningResource(
    "Jeff Erickson: Algorithms (free textbook)", "https://jeffe.cs.illinois.edu/teaching/algorithms/",
    "Chapters 5-9 cover graph basics, depth-first search, minimum spanning trees and shortest paths, with exercises."
)
private val mit6006 = LearningResource(
    "MIT OpenCourseWare 6.006 Introduction to Algorithms", "https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/",
    "Free lecture videos and notes. The graph lectures cover BFS, DFS, Bellman-Ford and Dijkstra."
)
private val winningWays = LearningResource(
    "Berlekamp, Conway, Guy: Winning Ways for your Mathematical Plays", "",
    "The classic reference on combinatorial games: Kayles, Dots and Boxes and the Sprague-Grundy theory."
)

/** Read before the lessons: vocabulary, representations and which algorithm to choose. */
val graphPrimer = TeachingGuide(
    summary = "A graph is a set of dots (vertices) joined by lines (edges). Most \"network\" questions are graph questions.",
    intuition = listOf(
        "Cities and roads, people and friendships, web pages and links: whenever things are connected, you can draw a graph. Vertices are the things and edges are the connections.",
        "Edges can be undirected (a two-way road) or directed (a one-way street, drawn as an arrow). They can carry a weight such as a distance, a price or a time.",
        "Each lesson in this app answers a different question about the same kind of picture: \"What is the cheapest way from A to F?\" or \"Which parts of the network hold together?\"",
    ),
    terms = listOf(
        "Vertex (node)" to "One of the dots. On the boards here they are labelled A-F.",
        "Edge" to "A connection between two vertices. With weight w it costs w to travel along it.",
        "Directed graph" to "Edges have a direction: u → v lets you go from u to v, not back.",
        "Degree" to "The number of edges touching a vertex. The degrees always add up to 2 × (number of edges), because each edge has two ends.",
        "Path" to "A sequence of vertices where each consecutive pair is joined by an edge. It is simple if no vertex repeats.",
        "Cycle" to "A path that returns to where it started without reusing an edge.",
        "Connected" to "Every vertex can reach every other vertex. In a directed graph the strong version requires paths in both directions.",
        "Tree" to "A connected graph with no cycles. A tree on V vertices always has exactly V - 1 edges.",
    ),
    exampleTitle = "Storing a graph in a program",
    exampleSteps = listOf(
        "Take the triangle A-B, B-C, C-A plus an extra edge C-D.",
        "Adjacency list: A: [B, C]   B: [A, C]   C: [A, B, D]   D: [C]. Uses O(V + E) memory, and you can list a vertex's neighbors in time proportional to its degree. Every algorithm in this app uses this format.",
        "Adjacency matrix: a V × V table with 1 where an edge exists. Uses O(V²) memory and answers \"is u joined to v?\" instantly. It suits dense graphs and very small boards.",
        "Edge list: [(A,B), (B,C), (C,A), (C,D)]. This is the simplest format and is what the road list under each board shows.",
    ),
    pseudocode = """
        // The traversal template behind BFS, Dijkstra and DFS.
        // Only the "frontier" data structure changes.
        function explore(graph, start):
            mark start as discovered
            frontier = { start }
            while frontier is not empty:
                u = frontier.take()          // queue -> BFS, stack -> DFS,
                                             // priority queue -> Dijkstra
                for each neighbor v of u:
                    if v is not discovered:
                        mark v as discovered
                        frontier.put(v)

        // Which algorithm should I use?
        //   every edge costs the same        -> BFS          O(V + E)
        //   edge costs are only 0 or 1       -> 0-1 BFS      O(V + E)
        //   costs are >= 0                   -> Dijkstra     O((V + E) log V)
        //   some costs are negative          -> Bellman-Ford O(V * E)
        //   "who can reach whom" (one-way)   -> SCC (Kosaraju / Tarjan)
        //   "which vertex is a weak point"   -> biconnected blocks / articulation points
    """.trimIndent(),
    complexity = listOf(
        "V = number of vertices, E = number of edges. Visiting each vertex and edge once costs O(V + E), the best you can do when you have to read the whole graph.",
        "A simple graph has at most V(V-1)/2 undirected edges, so E can be as large as about V²/2.",
    ),
    whyItWorks = "Every traversal discovers each vertex once and looks at each edge from both ends at most once. The order in which vertices leave the frontier is the only difference between the algorithms, and that order decides what the algorithm can guarantee: shortest by edge count, shortest by weight, or a depth-first structure that reveals cycles and components.",
    pitfalls = listOf(
        "Forgetting to mark vertices as discovered, which makes a traversal loop forever on a cycle.",
        "Treating a directed edge as two-way. In the SCC lesson, A → B does not let you travel from B to A.",
        "Assuming that the path with the fewest edges is also the cheapest. That holds only when every edge has the same cost.",
    ),
    quiz = listOf(
        QuizItem("A graph has 6 vertices with degrees 3, 3, 2, 2, 1, 1. How many edges does it have?", "The degrees sum to 12, and each edge adds 2 to that sum, so there are 12 / 2 = 6 edges."),
        QuizItem("A connected graph on 6 vertices has 5 edges. What kind of graph is it?", "A tree: a connected graph with exactly V - 1 edges has no cycles."),
        QuizItem("Every road costs 1 minute. Which algorithm should you use for the fastest route?", "BFS. With equal costs, the fastest route is the one with the fewest edges."),
    ),
    resources = listOf(
        LearningResource("VisuAlgo: graph data structures", "https://visualgo.net/en/graphds", "Draw a graph and see its adjacency matrix, adjacency list and edge list update live."),
        LearningResource("Wikipedia: Graph theory", "https://en.wikipedia.org/wiki/Graph_theory", "History (Euler's bridges of Königsberg) and core definitions."),
        erickson, mit6006, clrs,
    ),
    inThisApp = "LessonGraph in Graphs.kt stores an edge list and builds a cached adjacency list (neighbors()).",
)

val GraphTopic.guide: TeachingGuide get() = when (this) {
    GraphTopic.DIJKSTRA -> TeachingGuide(
        summary = "Always expand the closest unfinished place next. With non-negative costs, the first time you finish a vertex, its distance is final.",
        intuition = listOf(
            "Pour water into the network at A. It reaches places in order of travel cost, so the cheapest place gets wet first, then the next cheapest, and so on.",
            "Dijkstra keeps a tentative distance for each vertex: the best cost found so far. It repeatedly takes the unfinished vertex with the smallest tentative distance, marks it settled, and tries to improve its neighbors. Trying to improve a neighbor is called relaxing an edge.",
            "A route with fewer roads can cost more. Dijkstra compares total cost, never the number of roads.",
        ),
        terms = listOf(
            "Tentative distance dist[v]" to "The cheapest cost found so far from the start to v. It starts at infinity.",
            "Relax edge (u, v, w)" to "If dist[u] + w < dist[v], a cheaper way to v goes through u, so update dist[v] and remember parent[v] = u.",
            "Settled vertex" to "A vertex whose distance is final. Dijkstra settles one vertex per round.",
            "Parent pointers" to "parent[v] is the vertex you came from. Following them back from F rebuilds the route.",
        ),
        exampleTitle = "Trace on roads A-B (4), A-C (1), C-B (2), B-F (1), C-F (6)",
        exampleSteps = listOf(
            "Start: dist A = 0, every other distance = ∞.",
            "Settle A (0). Relax: B = 0+4 = 4, C = 0+1 = 1.",
            "Settle C (1), the smallest unsettled distance. Relax: B = min(4, 1+2) = 3 (improved), F = 1+6 = 7.",
            "Settle B (3). Relax: F = min(7, 3+1) = 4 (improved).",
            "Settle F (4). Done. The parents give F ← B ← C ← A, so the best route is A → C → B → F with cost 4.",
            "The route A → C → F uses only two roads but costs 7. Fewer roads did not mean cheaper.",
        ),
        pseudocode = """
            function dijkstra(graph, source):
                for each vertex v:
                    dist[v] = INF
                    parent[v] = none
                dist[source] = 0
                pq = min-priority-queue with (0, source)
                while pq is not empty:
                    (d, u) = pq.popMin()
                    if d > dist[u]: continue        // outdated entry, skip it
                    for each edge (u, v, w):
                        if dist[u] + w < dist[v]:   // relax
                            dist[v] = dist[u] + w
                            parent[v] = u
                            pq.push((dist[v], v))
                return dist, parent

            function rebuildPath(parent, target):
                path = []
                v = target
                while v != none:
                    path.addFront(v)
                    v = parent[v]
                return path
        """.trimIndent(),
        complexity = listOf(
            "Binary heap: O((V + E) log V). This is the standard choice.",
            "Plain array scan (used in this app because V = 6): O(V² + E). This is fine, even good, for dense graphs.",
            "Memory: O(V + E).",
        ),
        whyItWorks = "Suppose u has the smallest tentative distance among unsettled vertices. Any other route to u must leave the settled region at some unsettled vertex x, and dist[x] ≥ dist[u]. The remaining edges cost at least 0, so that route cannot beat dist[u]. That is why settling u is safe. The argument needs every weight to be at least 0: a negative edge could make a settled distance wrong after the fact.",
        pitfalls = listOf(
            "Using Dijkstra with negative edges. Use Bellman-Ford instead, or Johnson's algorithm for all pairs.",
            "Not skipping outdated priority-queue entries (the `if d > dist[u]` line). The answers stay correct, but the algorithm does extra work.",
            "Stopping as soon as F is discovered. F is final only when it is settled, that is, popped from the queue.",
            "Overflow: adding a weight to INF. Check dist[u] != INF before relaxing.",
        ),
        quiz = listOf(
            QuizItem("In the trace, dist[B] was 4 and then became 3. Why was it allowed to change?", "B had not been settled yet. A tentative distance may go down until its vertex is settled, and after that it never changes."),
            QuizItem("Add an edge C-F with cost -10. Why can Dijkstra fail?", "F might be settled before the cheaper route through the negative edge is found, and Dijkstra never revisits a settled vertex. Use Bellman-Ford instead."),
            QuizItem("If every cost equals 1, which simpler algorithm gives the same answer?", "BFS. Its FIFO queue already processes vertices in order of distance, so no priority queue is needed."),
        ),
        resources = listOf(
            LearningResource("CP-Algorithms: Dijkstra", "https://cp-algorithms.com/graph/dijkstra.html", "Proof, O(V²) and heap versions, and path restoration code."),
            LearningResource("VisuAlgo: single-source shortest paths", "https://visualgo.net/en/sssp", "Step through Dijkstra and Bellman-Ford on graphs you draw."),
            LearningResource("Wikipedia: Dijkstra's algorithm", "https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm", "History (1956/1959), variants and related algorithms (A*)."),
            clrs, erickson,
        ),
        inThisApp = "shortestDistances(graph, start, GraphTopic.DIJKSTRA) in Graphs.kt, using the array-scan version.",
    )

    GraphTopic.BFS -> TeachingGuide(
        summary = "Explore in rings: first everything 1 road away, then everything 2 roads away. The first time you reach a vertex is the fastest way there.",
        intuition = listOf(
            "Drop a stone in a pond. The ripples reach nearby places first. BFS does the same: layer 0 is the start, layer 1 its neighbors, layer 2 their new neighbors, and so on.",
            "A FIFO queue (first in, first out) makes this automatic. Vertices are added to the queue in layer order, so they are also processed in layer order.",
        ),
        terms = listOf(
            "Queue (FIFO)" to "Add at the back and remove from the front, like a line at a shop.",
            "Layer / level" to "All vertices at the same distance from the start.",
            "BFS tree" to "The edges (parent[v], v). Every tree path from the start is a shortest path.",
            "Unweighted graph" to "Every edge counts as 1. In this lesson every road takes one turn.",
        ),
        exampleTitle = "Trace on A-B, A-C, B-C, B-D, C-E, D-F, E-F",
        exampleSteps = listOf(
            "Queue [A], dist A = 0.",
            "Take A. Discover B (1) and C (1). Queue [B, C].",
            "Take B. C is already discovered. Discover D (2). Queue [C, D].",
            "Take C. Discover E (2). Queue [D, E].",
            "Take D. Discover F (3) with parent D. Queue [E, F].",
            "Take E. F is already discovered, so it is not changed. Take F. The queue is empty.",
            "F ← D ← B ← A: route A → B → D → F with 3 roads. A → C → E → F is equally short. Any shortest route counts as correct.",
        ),
        pseudocode = """
            function bfs(graph, source):
                for each vertex v:
                    dist[v] = INF
                    parent[v] = none
                dist[source] = 0
                queue = [source]
                while queue is not empty:
                    u = queue.removeFirst()
                    for each neighbor v of u:
                        if dist[v] == INF:          // first discovery
                            dist[v] = dist[u] + 1
                            parent[v] = u
                            queue.addLast(v)
                return dist, parent
        """.trimIndent(),
        complexity = listOf(
            "Time O(V + E): each vertex enters the queue once and each edge is checked from both ends.",
            "Memory O(V) for dist, parent and the queue.",
        ),
        whyItWorks = "At every moment the queue holds vertices at distance d followed by vertices at distance d + 1, never anything else. So vertices leave the queue in non-decreasing distance order, and when v is first discovered from u, no shorter route to v can still be waiting.",
        pitfalls = listOf(
            "Marking a vertex visited when it is removed instead of when it is added. It can then enter the queue several times.",
            "Using a stack instead of a queue. That is depth-first search, and its distances are not shortest.",
            "Using BFS on weighted roads. Use 0-1 BFS or Dijkstra.",
        ),
        quiz = listOf(
            QuizItem("Replace the queue with a stack. Is the result still a shortest-path algorithm?", "No. A stack gives a depth-first order, which can reach a vertex by a long detour first."),
            QuizItem("How do you get the actual route, not just its length?", "Store parent[v] when v is discovered, then follow the parents from F back to A and reverse the list."),
            QuizItem("A board has two shortest routes to F. Which one does BFS return?", "The one whose last step was discovered first, which depends on the neighbor order. Both are correct."),
        ),
        resources = listOf(
            LearningResource("CP-Algorithms: Breadth-first search", "https://cp-algorithms.com/graph/breadth-first-search.html", "Implementation plus applications: components, bipartiteness, shortest cycles."),
            LearningResource("VisuAlgo: DFS and BFS", "https://visualgo.net/en/dfsbfs", "Animated traversal showing the queue contents."),
            LearningResource("Wikipedia: Breadth-first search", "https://en.wikipedia.org/wiki/Breadth-first_search", "Pseudocode, history and properties."),
            mit6006, clrs,
        ),
        inThisApp = "shortestDistances(graph, start, GraphTopic.BFS) in Graphs.kt. It ignores the stored weights and counts every edge as 1.",
    )

    GraphTopic.ZERO_ONE -> TeachingGuide(
        summary = "If every road costs 0 or 1, a double-ended queue can replace Dijkstra's heap: free roads go to the front, paid roads go to the back.",
        intuition = listOf(
            "A zero-cost portal keeps you at the same distance, so the vertex it reaches belongs in the current layer. Put it at the front of the deque so it is processed now.",
            "A cost-1 road moves you to the next layer, so put that vertex at the back, as in BFS.",
            "Unlike BFS, the first discovery is not always final. A vertex found by a paid road can later be reached for free, so we keep checking whether its distance improves.",
        ),
        terms = listOf(
            "Deque" to "A double-ended queue. You can add and remove at both ends in O(1).",
            "Relaxation" to "Same as in Dijkstra: if dist[u] + w < dist[v], update dist[v].",
            "Stale entry" to "A vertex can sit in the deque twice. The later copy is harmless and just finds nothing to improve.",
        ),
        exampleTitle = "Trace on A-B (1), A-C (0), C-B (0), B-F (1)",
        exampleSteps = listOf(
            "Deque [A], dist A = 0.",
            "Take A. B = 1, paid road, goes to the back. C = 0, free, goes to the front. Deque [C, B].",
            "Take C. B: 0+0 = 0 < 1, so it improves. B goes to the front. Deque [B, B].",
            "Take B (0). F = 0+1 = 1, goes to the back. Deque [B, F].",
            "Take the stale B. Nothing improves. Take F. The answer is cost 1 via A → C → B → F.",
            "Compare plain BFS rules, which freeze B at its first distance of 1: they would give F = 2. That is why 0-1 BFS keeps relaxing.",
        ),
        pseudocode = """
            function zeroOneBfs(graph, source):
                for each vertex v: dist[v] = INF
                dist[source] = 0
                deque = [source]
                while deque is not empty:
                    u = deque.removeFirst()
                    for each edge (u, v, w):          // w is 0 or 1
                        if dist[u] + w < dist[v]:
                            dist[v] = dist[u] + w
                            if w == 0: deque.addFirst(v)
                            else:      deque.addLast(v)
                return dist
        """.trimIndent(),
        complexity = listOf(
            "Time O(V + E): a vertex is pushed only when its distance improves, and with 0/1 weights that happens a bounded number of times.",
            "Memory O(V + E) in the worst case for the deque.",
        ),
        whyItWorks = "The deque always holds distances d, d, ..., d+1, d+1 from front to back: never decreasing, with at most two different values. Adding a free neighbor (d) at the front and a paid one (d+1) at the back keeps this order. So vertices leave in non-decreasing distance order, the same guarantee Dijkstra's heap gives, but each operation costs O(1).",
        pitfalls = listOf(
            "Using a \"visited when discovered\" flag as in BFS. That freezes wrong distances (see the last step of the trace).",
            "Allowing weights other than 0 and 1. The two-value deque order breaks. This app's code rejects such boards.",
            "Pushing a free neighbor to the back. The algorithm then behaves like BFS and can give wrong answers.",
        ),
        quiz = listOf(
            QuizItem("Weights are only 0 and 5. Can you still use 0-1 BFS?", "Yes. Divide every weight by 5, solve, and multiply the answer by 5. Only the ratio between the two weights matters."),
            QuizItem("Weights are 1 and 2. What could you do?", "Split each cost-2 road into two cost-1 roads with a dummy vertex in the middle and run plain BFS, or just use Dijkstra."),
            QuizItem("How is 0-1 BFS related to Dijkstra?", "It is Dijkstra whose priority queue never holds more than two distance values, so a deque is enough."),
        ),
        resources = listOf(
            LearningResource("CP-Algorithms: 0-1 BFS", "https://cp-algorithms.com/graph/01_bfs.html", "Explanation, proof idea, and the Dial's-algorithm generalisation."),
            LearningResource("Codeforces: 0-1 BFS tutorial", "https://codeforces.com/blog/entry/22276", "Community tutorial with practice problems."),
            LearningResource("CP-Algorithms: Dijkstra", "https://cp-algorithms.com/graph/dijkstra.html", "The general algorithm that 0-1 BFS specialises."),
            erickson,
        ),
        inThisApp = "shortestDistances(graph, start, GraphTopic.ZERO_ONE) in Graphs.kt. It requires all weights to be 0 or 1.",
    )

    GraphTopic.BICONNECTED -> TeachingGuide(
        summary = "A block is a maximal piece of the network that survives the loss of any single vertex. Blocks meet at weak points called articulation vertices.",
        intuition = listOf(
            "Imagine any one intersection closing. If people inside a district can still reach each other, the district is resilient. A cycle is resilient: remove one vertex and the rest is still a path.",
            "An articulation vertex is a single point of failure. Removing it splits the graph. It belongs to every block it joins, which is why the same vertex can appear in several of your answers.",
            "A bridge is an edge whose removal disconnects the graph. Its two endpoints form a tiny two-vertex block.",
        ),
        terms = listOf(
            "Articulation vertex (cut vertex)" to "A vertex whose removal increases the number of connected components.",
            "Biconnected block" to "A maximal set of vertices with no articulation vertex inside it. Blocks may share vertices but never edges.",
            "Bridge" to "An edge whose removal disconnects its endpoints. In this lesson it forms a 2-vertex block.",
            "disc[v]" to "The DFS discovery time: when v was first visited.",
            "low[v]" to "The smallest discovery time reachable from v's DFS subtree using at most one back edge. It measures how high the subtree can climb without passing through v's parent.",
            "Back edge" to "A non-tree edge from a vertex to one of its DFS ancestors. It closes a cycle.",
        ),
        exampleTitle = "Trace on A-B, B-C, C-A, C-D, D-E, E-C, E-F (neighbors in alphabetical order)",
        exampleSteps = listOf(
            "DFS: A (disc 0) → B (1) → C (2). C sees A, a back edge: low C = 0. Push edges A-B, B-C, C-A.",
            "C → D (3) → E (4). E sees C, a back edge: low E = 2. Push C-D, D-E, E-C.",
            "E → F (5). F has no other neighbors, so low F = 5 ≥ disc E = 4. Pop down to E-F: block {E, F}, a bridge.",
            "Back at D: low D = min(3, low E = 2) = 2. Back at C: low D = 2 ≥ disc C = 2. Pop down to C-D: block {C, D, E}.",
            "Back at A: low B = 0 ≥ disc A = 0. Pop down to A-B: block {A, B, C}.",
            "Result: {A,B,C}, {C,D,E}, {E,F}. C and E appear twice, so they are the articulation vertices.",
        ),
        pseudocode = """
            time = 0
            function visit(u, parent):
                disc[u] = time; low[u] = time; time = time + 1
                for each neighbor v of u:
                    if disc[v] is unset:                    // tree edge
                        stack.push((u, v))
                        visit(v, u)
                        low[u] = min(low[u], low[v])
                        if low[v] >= disc[u]:               // u cuts off v's subtree
                            block = {}
                            repeat:
                                (x, y) = stack.pop()
                                block.add(x); block.add(y)
                            until (x, y) == (u, v)
                            output block
                    else if v != parent and disc[v] < disc[u]:  // back edge
                        stack.push((u, v))
                        low[u] = min(low[u], disc[v])

            for each vertex u:
                if disc[u] is unset:
                    if u has no neighbors: output {u}       // singleton block
                    visit(u, none)

            // Bonus facts from the same DFS:
            //   non-root u is an articulation vertex  <=>  some child v has low[v] >= disc[u]
            //   the root is an articulation vertex    <=>  it has 2 or more DFS children
            //   tree edge (u, v) is a bridge          <=>  low[v] >  disc[u]
        """.trimIndent(),
        complexity = listOf(
            "Time O(V + E): one DFS, and every edge is pushed and popped once.",
            "Memory O(V + E). The recursion can be V levels deep, so use an explicit stack for huge graphs.",
        ),
        whyItWorks = "If low[v] ≥ disc[u], no edge from v's subtree reaches above u, so every route from that subtree to the rest of the graph passes through u. The edges pushed since (u, v) are exactly the edges of that region, and together with u they form one maximal block. If low[v] < disc[u], a back edge climbs past u, forming a cycle through u's parent, so the region keeps growing upward.",
        pitfalls = listOf(
            "Confusing vertex-biconnected with 2-edge-connected. Two triangles sharing one vertex (a bowtie) have no bridge, but they form two blocks.",
            "Giving each vertex a single block label. Articulation vertices belong to several blocks, so store blocks as sets of edges or vertices.",
            "Using disc[v] versus low[v] incorrectly: back edges update with disc[v], tree children update with low[v].",
            "Forgetting isolated vertices. They are their own singleton blocks.",
        ),
        quiz = listOf(
            QuizItem("Which vertices are articulation vertices in the trace graph?", "C and E. Removing C separates {A, B} from {D, E, F}, and removing E separates F."),
            QuizItem("Two triangles share vertex X. How many blocks are there, and how many bridges?", "Two blocks (one per triangle) and no bridges. X is an articulation vertex."),
            QuizItem("Why does the test for a bridge use > while the block test uses ≥?", "low[v] = disc[u] means a back edge reaches u itself, forming a cycle through the edge (u, v), so that edge is not a bridge. But u still separates v's subtree from anything above u."),
        ),
        resources = listOf(
            LearningResource("CP-Algorithms: Articulation points", "https://cp-algorithms.com/graph/cutpoints.html", "The low-link DFS, with code."),
            LearningResource("CP-Algorithms: Finding bridges", "https://cp-algorithms.com/graph/bridge-searching.html", "The edge version of the same idea."),
            LearningResource("Wikipedia: Biconnected component", "https://en.wikipedia.org/wiki/Biconnected_component", "Definitions, the block-cut tree, and the Hopcroft-Tarjan algorithm (1973)."),
            erickson,
        ),
        inThisApp = "biconnectedComponents(graph) in Graphs.kt: low-link DFS with an edge stack, as above.",
    )

    GraphTopic.SCC -> TeachingGuide(
        summary = "With one-way streets, an \"island\" is a maximal group where everyone can reach everyone and get back. Kosaraju finds every group with two depth-first searches.",
        intuition = listOf(
            "A → B and B → A make a return trip possible, so A and B belong together. A → B alone does not: you can leave, but not come back.",
            "Shrink each strongly connected component to one dot and you get a graph with no cycles: the condensation. If it had a cycle, those components would merge into one.",
            "Every vertex is in exactly one SCC. A vertex with no return route to anyone is a group of one.",
        ),
        terms = listOf(
            "Strongly connected" to "For every pair u, v there is a directed path u ⇝ v and another v ⇝ u.",
            "SCC" to "A maximal strongly connected set of vertices.",
            "Finishing order" to "The order in which DFS finishes vertices, that is, after all their descendants.",
            "Reverse graph" to "The same vertices with every arrow flipped.",
            "Condensation (DAG)" to "A graph with one vertex per SCC. It is always acyclic.",
        ),
        exampleTitle = "Trace on A→B, B→C, C→A, C→D, D→E, E→F, F→D",
        exampleSteps = listOf(
            "Pass 1, DFS from A: A → B → C → (A seen) → D → E → F → (D seen).",
            "Finishing order: F, E, D, C, B, A. A finishes last.",
            "Reverse all arrows: B→A, C→B, A→C, D→C, E→D, F→E, D→F.",
            "Pass 2 in reverse finishing order, starting with A: A → C → B. D is not reachable in the reversed graph. SCC {A, B, C}.",
            "Next unvisited vertex in the order is D: D → (C already used) → F → E. SCC {D, E, F}.",
            "C → D is the only road between the groups. It goes one way, so the groups stay separate.",
        ),
        pseudocode = """
            function kosaraju(graph):
                seen = {}; order = []
                function dfs1(u):
                    seen.add(u)
                    for each edge u -> v:
                        if v not in seen: dfs1(v)
                    order.append(u)                 // u is finished

                for each vertex u:
                    if u not in seen: dfs1(u)

                rev = graph with every edge reversed
                seen = {}; components = []
                function dfs2(u, comp):
                    seen.add(u); comp.add(u)
                    for each edge u -> v in rev:
                        if v not in seen: dfs2(v, comp)

                for u in order from LAST to FIRST:
                    if u not in seen:
                        comp = {}
                        dfs2(u, comp)
                        components.add(comp)
                return components

            // Alternative: Tarjan's algorithm finds SCCs in ONE DFS using low-links,
            // the same idea as the biconnected lesson.
        """.trimIndent(),
        complexity = listOf(
            "Time O(V + E): two DFS passes plus building the reverse graph.",
            "Memory O(V + E) for the reverse graph. Tarjan's version avoids building it.",
        ),
        whyItWorks = "The vertex that finishes last lies in a source component of the condensation: nothing outside it points in. In the reversed graph that component becomes a sink, so a DFS started there cannot escape and collects exactly that SCC. Remove it, and the next-latest finisher is in the next source. Repeat.",
        pitfalls = listOf(
            "Running pass 2 on the original graph instead of the reversed one. The DFS leaks into downstream components.",
            "Running pass 2 in increasing finishing order instead of decreasing.",
            "Calling a group strongly connected because it is connected when you ignore the arrows. In the SCC lesson, check that every member can get back.",
        ),
        quiz = listOf(
            QuizItem("Add the road F → A to the trace graph. How many SCCs are there now?", "One. A ⇝ F already existed, and F → A provides the way back, so all six vertices merge."),
            QuizItem("What are the SCCs of an undirected graph?", "Its ordinary connected components, because every road works both ways."),
            QuizItem("Can the condensation graph contain a cycle?", "No. If components X and Y could each reach the other, they would be a single SCC."),
        ),
        resources = listOf(
            LearningResource("CP-Algorithms: Strongly connected components", "https://cp-algorithms.com/graph/strongly-connected-components.html", "Kosaraju with a proof, and condensation."),
            LearningResource("Wikipedia: Kosaraju's algorithm", "https://en.wikipedia.org/wiki/Kosaraju%27s_algorithm", "Step-by-step description and complexity."),
            LearningResource("Wikipedia: Tarjan's SCC algorithm", "https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm", "One-pass alternative using low-links."),
            clrs, erickson,
        ),
        inThisApp = "stronglyConnectedComponents(graph) in Graphs.kt, which uses Kosaraju.",
    )
}

val DuelKind.guide: TeachingGuide get() = when (this) {
    DuelKind.PLANAR -> TeachingGuide(
        summary = "A graph is planar if it can be drawn with no crossing edges. Its forbidden patterns are subdivisions of K5 and K3,3 (or those graphs as minors).",
        intuition = listOf(
            "Planarity asks whether some crossing-free drawing exists. After a legal edge is added, the dots are rearranged when needed to show one.",
            "Each move type constrains the other: an edge between two vertices forbids them from sharing a color later, and colored vertices forbid edges between equal colors.",
            "The game ends only when both move types are exhausted. The last player able to move wins.",
        ),
        terms = listOf(
            "Planar graph" to "A graph that can be drawn in the plane with no edges crossing.",
            "Face" to "A region bounded by edges in a planar drawing, including the outer region.",
            "Euler's formula" to "For a connected planar drawing: V - E + F = 2.",
            "K5" to "Five vertices, all ten pairs joined.",
            "K3,3" to "Two groups of three, every vertex joined to every vertex of the other group (the \"three utilities\" puzzle).",
            "Minor" to "A graph obtained by deleting edges or vertices and contracting edges.",
        ),
        exampleTitle = "How many edges can fit?",
        exampleSteps = listOf(
            "From Euler's formula, every simple planar graph with V ≥ 3 has E ≤ 3V - 6.",
            "V = 5 allows at most 9 edges. K5 has 10, so the 10th edge between five vertices is always rejected.",
            "V = 6 allows at most 12 edges. You can reach 12, for example with the octahedron graph.",
            "K3,3 has 9 ≤ 12 edges but is still not planar. It has no triangles, which gives the stronger bound E ≤ 2V - 4 = 8.",
            "Edge counting gives a quick rejection, but a full planarity test is still needed.",
        ),
        pseudocode = """
            function canColor(v, c):
                return v is uncolored and no neighbor of v has color c

            function canAdd(a, b):
                if a == b or edge (a, b) exists: return false
                if a and b are colored with the same color: return false
                return isPlanar(graph + edge(a, b))

            function isSmallPlanar(G):          // exact for V <= 6
                if V < 5: return true
                if some 5 vertices are pairwise adjacent: return false      // K5
                if contracting one edge yields a K5: return false           // K5 minor
                if some 3 + 3 split is fully joined: return false           // K3,3
                return true

            function isPlanarGraph(G):
                if V <= 6: return isSmallPlanar(G)
                if E > 3V - 6: return false
                if a K5 or K3,3 subdivision exists: return false
                return true

            after each accepted move:
                if the next player has no canColor and no canAdd move:
                    the player who just moved wins
        """.trimIndent(),
        complexity = listOf(
            "The small test checks all 5-vertex subsets, every edge contraction, and all 20 ways to split 6 vertices into 3 + 3, which is instant for V ≤ 6.",
            "For 7–10 vertices the app searches for K5 and K3,3 subdivisions. The search is exact at this size; it is not designed for large graphs.",
            "General graphs: the Hopcroft-Tarjan and Boyer-Myrvold algorithms test planarity in O(V) time.",
        ),
        whyItWorks = "Kuratowski's theorem says G is planar if and only if it has neither a K5 nor a K3,3 subdivision. For at most 6 vertices the minor shortcut is exact; for 7–10 vertices the app checks subdivisions directly. The four-colour theorem adds that every planar graph can be properly coloured with 4 colours, but in this game the players choose colours adversarially.",
        pitfalls = listOf(
            "Judging an edge only from the current positions. A legal edge may require moving the dots.",
            "Assuming the game ends when all vertices are colored. Edges may still be addable.",
            "Using the small six-vertex shortcut on larger boards; the larger boards need the subdivision test.",
        ),
        quiz = listOf(
            QuizItem("Five vertices, nine edges, all uncolored. Can a 10th edge ever be added?", "No. Ten edges on 5 vertices is exactly K5, which is not planar."),
            QuizItem("Why is K3,3 non-planar even though it has only 9 edges?", "It has no triangles, so every face has at least 4 sides, which forces E ≤ 2V - 4 = 8 < 9."),
            QuizItem("With one color (k = 1), what does coloring a vertex do to future edges?", "Two colored vertices can never be joined, because they would share the only color. Coloring therefore blocks edges."),
        ),
        resources = listOf(
            LearningResource("Wikipedia: Planar graph", "https://en.wikipedia.org/wiki/Planar_graph", "Euler's formula, edge bounds, and planarity testing."),
            LearningResource("Wikipedia: Kuratowski's theorem", "https://en.wikipedia.org/wiki/Kuratowski%27s_theorem", "K5 and K3,3 as the forbidden subdivisions, and Wagner's minor version."),
            LearningResource("Wikipedia: Four color theorem", "https://en.wikipedia.org/wiki/Four_color_theorem", "Why 4 colors always suffice for planar maps."),
            LearningResource("NetworkX check_planarity", "https://networkx.org/documentation/stable/reference/algorithms/generated/networkx.algorithms.planarity.check_planarity.html", "A general-purpose implementation to experiment with in Python."),
        ),
        inThisApp = "isPlanarGraph() and PlanarGame in PlanarGame.kt; redraws in PlanarLayout.kt.",
    )

    DuelKind.COLORING -> TeachingGuide(
        summary = "Whether a graph can be 3-colored is a different question from whether the Builder can force a 3-coloring against an opponent.",
        intuition = listOf(
            "A vertex is in danger when its neighbors already use two of the three colors. One more move by the Blocker, giving its last free color to a neighbor, kills it.",
            "One dead vertex decides the game. The Builder can no longer color everything, even if play continues for a while.",
            "Builder strategy: color the most endangered vertices first. Blocker strategy: spread different colors around a vertex with many uncolored neighbors.",
        ),
        terms = listOf(
            "Proper coloring" to "Adjacent vertices always get different colors.",
            "Chromatic number χ(G)" to "The fewest colors that allow a proper coloring.",
            "Saturation" to "The number of distinct colors already used by a vertex's neighbors. With k colors, saturation k means the vertex is dead.",
            "Game chromatic number χg(G)" to "The fewest colors with which the Builder can guarantee a complete coloring against a Blocker. Always χg(G) ≥ χ(G).",
        ),
        exampleTitle = "A trap around a star",
        exampleSteps = listOf(
            "D is joined to A, B and C, and A, B, C are not joined to each other. The graph is easy to 2-color.",
            "Blocker colors A with 1. Builder ignores the danger and colors somewhere else.",
            "Blocker colors B with 2. D's saturation is now 2, and only color 3 is left for D.",
            "Builder must color D with 3 right away. If not, the Blocker colors C with 3 and D is dead.",
            "Lesson: chromatic number 2, yet careless play loses with 3 colors.",
        ),
        pseudocode = """
            function legal(v, c):
                return v uncolored and no neighbor of v has color c

            function saturation(v):
                return number of distinct colors among v's neighbors

            builderMove():                    // DSatur-style heuristic
                v = uncolored vertex with highest saturation
                    (break ties by most uncolored neighbors)
                c = legal color for v that removes the fewest options
                    from v's uncolored neighbors
                color v with c

            blockerMove():
                look for an uncolored v with saturation k - 1 and missing color c,
                    and an uncolored neighbor u of v where legal(u, c)
                if found: color u with c          // v is now dead
                else: color the vertex with most uncolored neighbors,
                      using a color its neighborhood does not have yet

            game end:
                every vertex colored            -> Builder wins
                no legal(v, c) anywhere         -> Blocker wins
        """.trimIndent(),
        complexity = listOf(
            "A legality check costs O(degree).",
            "Perfect play: search the game tree. With 6 vertices and 3 colors there are at most 4^6 = 4096 colorings, which can be memoized.",
        ),
        whyItWorks = "The game coloring problem was introduced by Steven Brams (popularized by Martin Gardner in 1981) and studied by Bodlaender (1991). Some families have known bounds: for example, every forest has game chromatic number at most 4 (Faigle, Kern, Kierstead and Trotter, 1993). Greedy saturation-first play is a heuristic, not a proof of optimality.",
        pitfalls = listOf(
            "Believing that χ(G) ≤ 3 means the Builder always wins with 3 colors.",
            "Counting saturation as the number of colored neighbors. It counts distinct colors.",
            "Forgetting that recoloring is forbidden. Every choice is permanent.",
        ),
        quiz = listOf(
            QuizItem("Can the Blocker win on a single edge A-B with 3 colors?", "No. Every vertex has at most 1 neighbor, so its saturation never reaches 3."),
            QuizItem("A vertex has 2 neighbors. Can it ever be killed with 3 colors?", "No. Two neighbors use at most 2 colors, so one always remains. Only vertices of degree ≥ 3 are at risk."),
            QuizItem("Why does one dead vertex already decide the game?", "The Builder needs every vertex colored, and a dead vertex can never be colored."),
        ),
        resources = listOf(
            LearningResource("Wikipedia: Graph coloring game", "https://en.wikipedia.org/wiki/Graph_coloring_game", "Rules, game chromatic number, and known bounds."),
            LearningResource("Wikipedia: DSatur", "https://en.wikipedia.org/wiki/DSatur", "The saturation-first greedy coloring heuristic."),
            LearningResource("Wikipedia: Graph coloring", "https://en.wikipedia.org/wiki/Graph_coloring", "Chromatic number, greedy coloring, and applications."),
        ),
        inThisApp = "DuelState.color() and legalColor() in GraphDuels.kt.",
    )

    DuelKind.SHANNON -> TeachingGuide(
        summary = "Connector wants a path and Cutter wants a cut. The winner is decided by redundancy: how many disjoint ways A and F are joined.",
        intuition = listOf(
            "Every A-F path must cross every cut. A cut is a set of edges whose removal separates A from F. The two players race to build one or the other.",
            "A single route is fragile: one cut and it is gone. Connector needs a structure where every cut can be answered by a repair.",
            "The pure ring (two separate 3-road routes) is a Cutter win. Chords that link the two routes are what give the Connector a chance.",
        ),
        terms = listOf(
            "Cut" to "A set of edges whose removal disconnects A from F.",
            "Edge-disjoint paths" to "Paths that share no edge.",
            "Menger's theorem" to "The largest number of edge-disjoint A-F paths equals the smallest cut size.",
            "Spanning tree" to "A tree using edges of the graph that connects a given set of vertices.",
        ),
        exampleTitle = "Why the plain ring loses for Connector",
        exampleSteps = listOf(
            "The ring A-B-C-F-E-D-A gives two routes: P = A-B-C-F and Q = A-D-E-F, each 3 edges.",
            "Connector claims A-B on P.",
            "Cutter cuts D-E. Q is dead, so only P remains, and it still needs 2 claims.",
            "Connector claims B-C. Cutter cuts C-F. No A-F route is left, so Cutter wins.",
            "Add the chord B-E, and Connector has extra routes to answer the cuts. Try it on a random board.",
        ),
        pseudocode = """
            function connected(edges):        // BFS from A using only these edges
                return F is reachable from A

            after each move:
                if connected(claimed edges):       Connector wins
                if not connected(all uncut edges): Cutter wins

            // Lehman's winning strategy for Connector (1964):
            // Assume two edge-disjoint trees T1, T2 that both span a vertex set containing A and F.
            when Cutter deletes edge e:
                say e is in T1; removing e splits T1 into two parts
                pick an edge f of T2 that joins those two parts   // exists: T2 spans both
                claim f, and merge f's endpoints in both trees
                T1 - e + f and T2 - f are again two disjoint spanning trees
        """.trimIndent(),
        complexity = listOf(
            "Each win check is one BFS: O(V + E).",
            "Deciding whether two edge-disjoint spanning trees exist can be done in polynomial time with matroid algorithms. Unusually for a game, this one is fully solved.",
        ),
        whyItWorks = "Lehman showed that this game is solved by matroid theory. Connector wins, even moving second, exactly when some subgraph containing A and F has two edge-disjoint spanning trees. The repair strategy above keeps that structure intact after every cut, so the claimed edges eventually connect A and F.",
        pitfalls = listOf(
            "Claiming edges along the single cheapest-looking route. Redundancy matters more than length.",
            "As Cutter, cutting an edge that belongs to no surviving A-F route, which wastes a move.",
            "Forgetting that claimed edges are permanent and can never be cut.",
        ),
        quiz = listOf(
            QuizItem("A and F are joined by three separate paths, each 3 edges long, and nothing else. Who wins?", "Cutter. Whichever path Connector starts, Cutter cuts it next turn. Separate paths cannot repair each other, so Connector needs paths linked by extra edges."),
            QuizItem("What does Menger's theorem say about the plain 6-vertex ring?", "There are 2 edge-disjoint A-F paths, so a cut of 2 edges exists. Cutter, with the second move, reaches it in time."),
            QuizItem("Why is a cut edge useless if it lies on no remaining A-F route?", "The win condition only cares about A-F connectivity, so that cut does nothing."),
        ),
        resources = listOf(
            LearningResource("Wikipedia: Shannon switching game", "https://en.wikipedia.org/wiki/Shannon_switching_game", "History, Lehman's solution, and the connection to Gale/Bridg-It."),
            LearningResource("Wikipedia: Menger's theorem", "https://en.wikipedia.org/wiki/Menger%27s_theorem", "Disjoint paths versus minimum cuts."),
            LearningResource("CP-Algorithms: Maximum flow (Edmonds-Karp)", "https://cp-algorithms.com/graph/edmonds_karp.html", "Computing the minimum cut, which counts disjoint paths."),
        ),
        inThisApp = "DuelState.takeEdge() and the private connected() check in GraphDuels.kt.",
    )

    DuelKind.KAYLES -> TeachingGuide(
        summary = "Node Kayles is an impartial game: both players have the same moves, so Sprague-Grundy numbers tell you exactly who wins.",
        intuition = listOf(
            "Taking a vertex removes it and its neighbors. The chosen vertices form an independent set: no two are adjacent.",
            "Late in the game the board breaks into separate pieces. Each piece acts like a pile in Nim, and you combine the piles with XOR.",
            "Removing many vertices at once is not automatically good. What matters is leaving your opponent a position worth 0.",
        ),
        terms = listOf(
            "Closed neighborhood N[v]" to "v together with all its neighbors. This is what a move removes.",
            "Normal play" to "The player who cannot move loses.",
            "Impartial game" to "Both players have the same moves from every position.",
            "mex" to "Minimum excluded value: the smallest non-negative integer not in a set. For example, mex{0, 1, 3} = 2.",
            "Grundy value" to "g(position) = mex of the Grundy values of all positions one move away. g = 0 means the player to move loses.",
        ),
        exampleTitle = "Grundy values of paths",
        exampleSteps = listOf(
            "Empty board: g = 0.",
            "Path of 1 or 2 vertices: every move clears the board (value 0), so g = mex{0} = 1.",
            "Path A-B-C: taking B clears everything (0), and taking an end leaves one vertex (1). g = mex{0, 1} = 2.",
            "Path A-B-C-D: every move leaves a 1- or 2-vertex path, both worth 1. g = mex{1} = 0, so the player to move loses.",
            "Two separate 4-paths: 0 XOR 0 = 0, a loss. A 3-path plus a single vertex: 2 XOR 1 = 3, a win.",
        ),
        pseudocode = """
            memo = {}
            function grundy(alive):               // alive = set of remaining vertices
                if alive is empty: return 0
                if alive in memo: return memo[alive]
                options = {}
                for each v in alive:
                    options.add(grundy(alive minus N[v]))
                g = mex(options)
                memo[alive] = g
                return g

            function mex(s):
                m = 0
                while m in s: m = m + 1
                return m

            // The player to move wins  <=>  grundy(alive) != 0.
            // A winning move is any v with grundy(alive minus N[v]) == 0.
            // Separate pieces: grundy(X + Y) = grundy(X) XOR grundy(Y).
        """.trimIndent(),
        complexity = listOf(
            "Brute force over subsets: 2^V states, each trying V moves. For V = 6 that is only 64 states.",
            "In general, Node Kayles is PSPACE-complete (Schaefer, 1978), so no fast algorithm is expected for big graphs.",
        ),
        whyItWorks = "The Sprague-Grundy theorem says every impartial normal-play game is equivalent to a single Nim pile of size g. A position with g = 0 is lost for the player to move: every move goes to g ≠ 0, and from any g ≠ 0 there is a move back to 0. A sum of independent games behaves like Nim piles, whose combined value is the XOR.",
        pitfalls = listOf(
            "Choosing the move that removes the most vertices instead of the move that leaves g = 0.",
            "Forgetting that a move removes the chosen vertex's neighbors too.",
            "Adding Grundy values instead of XOR-ing them.",
        ),
        quiz = listOf(
            QuizItem("Triangle A-B-C. Who wins?", "The first player: any vertex removes all three, leaving the opponent with no move."),
            QuizItem("What is mex{0, 2, 3}?", "1, the smallest non-negative integer not in the set."),
            QuizItem("Pieces with Grundy values 1, 2 and 3. Is the player to move winning?", "No: 1 XOR 2 XOR 3 = 0, which is a losing position."),
        ),
        resources = listOf(
            LearningResource("CP-Algorithms: Sprague-Grundy theorem and Nim", "https://cp-algorithms.com/game_theory/sprague-grundy-nim.html", "Proof, mex, and worked examples."),
            LearningResource("Wikipedia: Kayles", "https://en.wikipedia.org/wiki/Kayles", "Kayles and Node Kayles, values and complexity."),
            LearningResource("Wikipedia: Sprague-Grundy theorem", "https://en.wikipedia.org/wiki/Sprague%E2%80%93Grundy_theorem", "The general statement."),
            winningWays,
        ),
        inThisApp = "DuelState.takeNode() in GraphDuels.kt. The Nim lesson in the game-theory module uses the same XOR idea.",
    )

    DuelKind.PATH -> TeachingGuide(
        summary = "Sabotage raises costs, and only a fresh run of Dijkstra after the change shows the new optimum. The Saboteur's best tool is a near-tie.",
        intuition = listOf(
            "Raising a road that no cheapest route uses changes nothing. The Saboteur should hit roads on the current best route.",
            "If two routes are close in cost, a small increase flips which one is best, and the Router has to notice the switch.",
            "A Router who recomputes carefully always wins. The challenge is to do Dijkstra in your head under pressure.",
        ),
        terms = listOf(
            "Shortest-path interdiction" to "Increasing or removing edges to make the shortest path as long as possible within a budget.",
            "Bottleneck edge" to "An edge shared by every shortest route. Raising it raises the optimum directly.",
            "Near-tie" to "Two routes whose costs differ by very little.",
        ),
        exampleTitle = "Moving the optimum",
        exampleSteps = listOf(
            "Suppose route P costs 6 and route Q costs 8, and they share no edge.",
            "Saboteur adds +3 to one edge of P. P now costs 9, and Q at 8 becomes optimal.",
            "If the Saboteur had spread the points over Q, the optimum would still be P at 6.",
            "If P and Q share an edge, raising it raises both. One point there is worth more.",
        ),
        pseudocode = """
            // Router: recompute after the board is locked.
            dist, parent = dijkstra(graph, A)
            route = rebuildPath(parent, F)         // see the Dijkstra lesson

            // Saboteur, greedy one-point-at-a-time heuristic.
            while budget > 0:
                bestEdge = none; bestValue = dijkstra(graph, A).dist[F]
                for each edge e:
                    e.weight += 1
                    value = dijkstra(graph, A).dist[F]
                    e.weight -= 1
                    if value > bestValue: bestEdge = e; bestValue = value
                if bestEdge == none: break           // ties: no single +1 helps
                bestEdge.weight += 1; budget -= 1
        """.trimIndent(),
        complexity = listOf(
            "One Dijkstra per candidate edge per point: O(budget · E · (V + E) log V). That is tiny here.",
            "Spending a budget optimally is studied as shortest-path interdiction. The greedy loop above is only a heuristic and can miss the best plan.",
        ),
        whyItWorks = "Dijkstra's guarantee is about one fixed set of weights. Once any weight changes, earlier distances may be wrong, so the Router must recompute from scratch. The greedy Saboteur can get stuck when several shortest routes tie, because a single +1 then raises nothing. Spending two points on different routes can still pay off.",
        pitfalls = listOf(
            "As Router, reusing the route that was best before sabotage.",
            "As Saboteur, raising edges that are not on any shortest route.",
            "Saving the budget. Unused points are allowed, but they do nothing.",
        ),
        quiz = listOf(
            QuizItem("The best route costs 7 and the second best costs 7 too. Saboteur raises one edge of the first. What is the new optimum?", "Still 7, because the other route is untouched. With ties, a single +1 is wasted unless it hits a shared edge."),
            QuizItem("Why can't the Router keep Dijkstra's old distances?", "They were computed for the old weights. Any settled distance may now be too small."),
            QuizItem("Which kind of edge gives the Saboteur the most value per point?", "A bottleneck edge on every shortest route, since each point raises the optimum by 1."),
        ),
        resources = listOf(
            LearningResource("CP-Algorithms: Dijkstra", "https://cp-algorithms.com/graph/dijkstra.html", "The algorithm the Router has to run."),
            LearningResource("Wikipedia: Shortest path problem", "https://en.wikipedia.org/wiki/Shortest_path_problem", "Variants, including dynamic and adversarial versions."),
            LearningResource("VisuAlgo: single-source shortest paths", "https://visualgo.net/en/sssp", "Change a weight and rerun to see the route switch."),
        ),
        inThisApp = "DuelState.raiseCost(), lockRoads(), travel() and submitRoute() in GraphDuels.kt.",
    )

    DuelKind.BOXES -> TeachingGuide(
        summary = "Never give away a third side unless you have to. When you have to, give away the smallest chain, and learn the double-cross to keep control.",
        intuition = listOf(
            "Completing a box gives you another turn. A box with three sides is a free point for whoever moves next.",
            "Late in the game every move gives something away. The board becomes a set of chains and loops, and players are forced to open them one by one.",
            "The player in control takes all but two boxes of each long chain, then hands those two over. The opponent must take them and then open the next chain.",
        ),
        terms = listOf(
            "Safe move" to "An edge that gives no box its third side.",
            "Chain" to "A corridor of boxes where opening one lets the opponent capture the whole line. A long chain has 3 or more boxes.",
            "Loop" to "A closed chain. Giving up control costs 4 boxes instead of 2.",
            "Double-dealing move" to "Declining the last two boxes of a chain, leaving them as a domino for the opponent.",
            "Double-cross" to "One edge that completes two boxes at once, usually the opponent taking that domino.",
        ),
        exampleTitle = "Keeping control in a 4-box chain",
        exampleSteps = listOf(
            "Your opponent is forced to open a 4-box chain.",
            "Greedy: take all 4, then you must open the next chain. You gain 4 but lose control.",
            "Double-dealing: take 2, then play the far end so the last 2 form a domino. The opponent takes 2 with one move and must open the next chain for you.",
            "Over several long chains, keeping control usually beats grabbing everything now.",
        ),
        pseudocode = """
            function play(edge):
                mark edge taken
                done = boxes next to edge whose 4 sides are now all taken
                give every box in done to the current player
                if done is empty: switch player     // completing a box keeps the turn
                if every edge is taken: compare scores

            function safe(edge):
                return no box next to edge would get its 3rd side

            strategy():
                if a box has 3 sides:
                    take it (unless double-dealing at the end of a long chain)
                else if some edge is safe:
                    play a safe edge
                else:
                    open the shortest chain or loop

            // Grid with r x c dots: r(c-1) + c(r-1) edges, (r-1)(c-1) boxes.
        """.trimIndent(),
        complexity = listOf(
            "Each move checks at most 2 adjacent boxes: O(1).",
            "Perfect play on big boards is hard. Barker and Korf (2012) needed heavy computer search to solve the 4×5-box board.",
        ),
        whyItWorks = "Berlekamp's long-chain rule: the first player wants the number of dots plus the number of long chains (as the board settles) to be even, and the second player wants it odd. The rule works because chain parity decides who must open the first long chain, and the player in control can then double-deal through the rest.",
        pitfalls = listOf(
            "Taking every box offered at the end. Count the remaining chains first.",
            "Opening a long chain while a short one is available.",
            "Forgetting that completing a box means you must move again, possibly into a bad position.",
        ),
        quiz = listOf(
            QuizItem("A 3×3-dot board: how many edges and boxes?", "3·2 + 3·2 = 12 edges and 2·2 = 4 boxes."),
            QuizItem("Why does completing two boxes with one edge give only one extra turn?", "The rule is to move again after completing at least one box. One move gives one extra turn."),
            QuizItem("When is taking all boxes of a chain correct?", "When it is the last chain, or when keeping control is worth less than the 2 boxes you would give up."),
        ),
        resources = listOf(
            LearningResource("Wikipedia: Dots and boxes", "https://en.wikipedia.org/wiki/Dots_and_boxes", "Rules, the double-cross, and the long-chain rule."),
            LearningResource("Elwyn Berlekamp: The Dots and Boxes Game (A K Peters, 2000)", "", "A short book of puzzles that teaches chain control."),
            winningWays,
        ),
        inThisApp = "DuelState.takeEdge() for DuelKind.BOXES in GraphDuels.kt.",
    )
}
