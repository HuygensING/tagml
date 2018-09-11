## multi-colored-tree traversals

# Find all markup nodes for a given text node

- initialize markup node set *ms*

- initialize nodes-to-process list *ntp*

- initialize nodes-handled set *nh*

- from text node *t<sub>1</sub>*, find all incoming markup - text edges *e<sub>1..n</sub>*

- for edges *e<sub>1</sub>* .. *e<sub>n</sub>*,
  - add the source nodes *m<sub>1</sub>* .. *m<sub>n</sub>* of the edges to *ntp*

- while *ntp* is not empty : 
  - remove the first markup node *m<sub>1</sub>* from *ntp*
  - add *m<sub>1</sub>* to *ms*
  - add *m<sub>1</sub>* to *nh*
  - if *m<sub>1</sub>* has incoming markup - markup edges *e<sub>1..n</sub>* :
     - add the source nodes *m<sub>1</sub>* .. *m<sub>n</sub>* of the edges to *ntp*
     - remove all nodes in *nh* from *ntp*
     
- *ms* now contains all markup nodes for the given text node. 


# Find all text nodes for a given markup node

- initialize text node set *ts*

- initialize nodes-to-process list *ntp*

- initialize nodes-handled set *nh*

- add markup node *m<sub>1</sub>* to *ntp*

- while *ntp* is not empty : 
  - remove the first node *n<sub>1</sub>* from *ntp*
  - if *n<sub>1</sub>* is a markup node :
    - add all child nodes of *n<sub>1</sub>* to the front of *ntp*
  - if *n<sub>1</sub>* is a text node :
    - add *n<sub>1</sub>* to *ts*
     
- *ts* now contains all text nodes for the given markup node. 


# Find all child nodes of a given markup node

- initialize node set *ns*

- initialize nodes-to-process list *ntp*

- add the given markup node *m<sub>i</sub>* to *ntp*

- if *m<sub>i</sub>* has incoming *continuation-edge* *e<sub>i</sub>*  

- for *e<sub>i</sub>* in all outgoing edges *e<sub>1</sub>* .. *e<sub>n</sub>*,
  - add target nodes *n<sub>1</sub>* .. *n<sub>n</sub>* to *ns*

- if the given markup node is part of a discontinued markup, add all child nodes of the continued markup node to *ns*

- *ns* now contains all child nodes for the given markup node. 