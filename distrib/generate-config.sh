if [ ! -f ../dist/org.bbi.Notify.jar ]; then
	cd ..
	ant jar
	cd distrib
fi
java -jar ../dist/org.bbi.Notify.jar --format | sed -n '/=\|\[.*\]/p' | sed 's/\(\[.*\]\)/\n\1/' | awk '{ print $1 }'

