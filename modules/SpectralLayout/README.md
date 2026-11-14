# Spectral Layout

*by [Johan Myrberger](https://www.linkedin.com/in/myrberger/)*

A Gephi layout that places every node at the coordinates given by **eigenvectors of the graph Laplacian**
(spectral graph drawing). The eigenvector of lambda 2 (the Fiedler vector) is the x axis and lambda 3 the
y axis by default. Clusters that are loosely connected to the rest of the network separate along these axes.

Theory and background: <https://johanm.github.io/graph-matrixmath-ona/graphmatrixmath.html>

## Settings (Layout panel)

| Setting | Meaning |
|---|---|
| X / Y axis: eigenvector λk | Index k of lambda_k. Default x = 2, y = 3. Any k works on any axis. 1 is the constant vector. **Type 0 for "none"** (see below). |
| X / Y axis: scale | Linear (default) or Log. Log is a symmetric logarithm, so negative values and zero are fine. |
| X / Y axis: show line | Draws the axis as a straight line (Preview and exports, see below). |
| X / Y axis: flip | Mirrors the axis. Eigenvectors have no natural sign. |
| Laplacian | Combinatorial (default), Normalised, or Random walk. See the next section. |
| Use edge weights | Weights act as spring strengths. |
| Axis half-width, Log axis: linear zone | Overall size, and how strongly a Log axis compresses. |
| Stack bins / spacing / center | Used when one axis is "none" (see below). |
| Eigenvectors to save, Save eigenvectors to node columns | Write eigenvectors to node columns (see below). |

## Which Laplacian?

All three work on the same graph; they differ in how node degree enters. The eigenvalues are ascending, λ1 is
always 0, and the number of zero eigenvalues equals the number of connected components. Write W for the
(weighted) adjacency matrix and D for the diagonal matrix of node degrees.

**Combinatorial** (default), `L = D - W`. The classic Laplacian from the theory page. λ2 is the "algebraic
connectivity" and its eigenvector minimises the sum of w·(x_i − x_j)² over all edges for a fixed length, that is,
connected nodes are pulled together. The eigenvectors are used as they are. Nodes with many connections
(hubs) have a strong influence on the result. Eigenvalues range from 0 up to about twice the largest degree.

**Normalised**, `L_n = D^-1/2 · L · D^-1/2`. The Laplacian is divided by node degree on both sides, so a hub
does not dominate merely because it has many edges. Eigenvalues always lie between 0 and 2, which makes them
comparable between networks. The eigenvectors of `L_n` are used as they are. This variant is the one behind
"normalised cut" community detection and is a good choice when degrees vary widely.

**Random walk**, `L_rw = D^-1 · L`. Closely related to the normalised one: it has the same eigenvalues, and its
eigenvectors are those of the normalised Laplacian with every node's value divided by √degree, so hubs get
relatively smaller coordinates. They are the eigenvectors that describe how a random walker spreads over the
network (the slowest-mixing directions), and the version used in most spectral clustering literature.

Rule of thumb: start with Combinatorial; try Normalised or Random walk if a few hubs dominate the picture or you
mainly want to see community structure. Nodes without edges get the eigenvalue 0 in all three.

## One axis set to "none"

If you type **0** in the eigenvector field of one axis (and run the layout again), the other axis shows the
**distribution** of the nodes: each node stays at its exact position along that axis, and nodes that fall in
the same bin are stacked on top of each other, like a dot plot. With both axes "none" the layout changes nothing.

## Axis lines and labels

Gephi's Overview window has no way for a plugin to draw lines, and a layout must not add helper nodes to your
data. The axis lines are therefore drawn by a **Preview renderer**: switch to the Preview tab and click
Refresh. They also appear in PNG, SVG and PDF export. Each line is labelled at its end with the eigenvalue index
and value and the scale, for example `λ2 = 0.697` or `λ3 = 1.38 (log)` (in PDF the Greek letter is written
"lambda"). Labels, label size, line thickness and color are under Preview Settings, "Spectral axes".
The lines mark where the other coordinate is zero and reflect the last layout run.

## Saving the eigenvectors to node columns

In the layout settings, in the last group, "Write eigenvectors to columns", set **Eigenvectors to save** (K) and then click the **"..."
button** on the row "Save eigenvectors to node columns". A small window opens; press **Save eigenvectors**.
The eigenvectors λ1 to λK of the visible graph are written to node columns (Data Laboratory), where they can be
used for coloring, ranking, filtering or export. The window lists the eigenvalues.

* Column names contain the order and the method: `eigenvector_2_combinatorial`, `eigenvector_3_normalized`,
  `eigenvector_2_randomwalk`. If "Use edge weights" is off, `_unweighted` is appended.
* Values are the unit-length eigenvectors with the sign convention "the entry with the largest magnitude is
  positive". They are not scaled to the layout's axis size.
* Saving uses the Laplacian and edge-weight settings of the layout, does not move any node, and overwrites
  columns with the same name from an earlier save.
* The new columns are added at the right end of the node table. In a wide Data Laboratory table you may need to
  scroll right or widen the window. They can also be used in Appearance > Ranking.
* A graph with n nodes has at most n eigenvectors, so a small graph gets fewer than K columns.
* Only the **visible** graph is used. With a filter active, the nodes outside the filter get no value.
  The window tells you when either of these applies.

## Good to know

* Edge direction is ignored; parallel edges add up; self-loops and weights that are zero, negative or not a
  number are ignored.
* If lambda_2 and lambda_3 are (nearly) equal, their eigenvectors are an arbitrary rotation of each other.
  The picture is still valid but the orientation is not meaningful.
* Up to 1000 nodes the eigenvectors are exact; above that an iterative solver (LOBPCG) is used.
  Eigenvalues are written to Gephi's log (gephi-log.txt).

## Build notes

`mvn -pl modules/SpectralLayout clean package`, then `mvn org.gephi:gephi-maven-plugin:run`.
Targets Gephi 0.11.4.
