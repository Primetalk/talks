render:
	find . -name "*.dot" -exec dot -Tpng '{}' -o '{}'.png \;
#	dot -Tpng persistent-data-structures/rbt1.dot -o persistent-data-structures/rbt1.png
#	dot -Tpng persistent-data-structures/left-heap.dot -o persistent-data-structures/left-heap.png
