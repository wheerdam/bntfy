#!/bin/bash
echo "- bntfy deb builder"
if [ -z "$(command -v fakeroot)" ]
then
  echo "- fakeroot not found"
  exit 1
fi
if [ ! -e ../dist/org.bbi.Notify.jar ]
then
	echo "- jar archive not found, building"
	cd ..
	ant jar
	cd distrib
fi
echo "- copying files to include in the package"
rm -rf ./deb/usr
mkdir ./deb/etc
mkdir ./deb/usr
mkdir ./deb/usr/bin
mkdir ./deb/usr/lib
mkdir ./deb/usr/lib/bntfy
cp -v ../dist/org.bbi.Notify.jar ./deb/usr/lib/bntfy/bntfy.jar
cp -v bntfy-vol.sh ./deb/usr/bin
cp -v bntfy-vol-pulse.sh ./deb/usr/bin
cp -v sample.conf ./deb/etc/bntfy.conf
echo "- generating run script"
echo "#!/bin/sh" > ./deb/usr/bin/bntfy
echo "java -jar /usr/lib/bntfy/bntfy.jar -c /etc/bntfy.conf \"\$@\"" >> ./deb/usr/bin/bntfy
chmod a+r ./deb/etc/bntfy.conf
chmod a+rx ./deb/usr/bin/bntfy
chmod a+rx ./deb/usr/bin/bntfy-vol.sh
chmod a+rx ./deb/usr/bin/bntfy-vol-pulse.sh
echo "- building deb"
fakeroot dpkg --build deb
mv -v deb.deb bntfy.deb
echo "- done"
