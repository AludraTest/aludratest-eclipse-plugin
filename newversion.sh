#!/bin/bash
#

# update version here and run script
PREV_VERSION=0.6.3
NEW_VERSION=0.6.4

PREV_VERSION_SHORT=$(echo $PREV_VERSION | sed s/\\.//g)
NEW_VERSION_SHORT=$(echo $NEW_VERSION | sed s/\\.//g)

function replaceInFile {
  cat "$1" | sed "s/$2/$3/g" > tmp.txt
  mv tmp.txt "$1"
}


echo "Updating basic files"

# update MANIFEST.MF
replaceInFile META-INF/MANIFEST.MF $PREV_VERSION.qualifier $NEW_VERSION.qualifier 

# update pom.xml
replaceInFile pom.xml $PREV_VERSION.qualifier $NEW_VERSION.qualifier 

# update feature/pom.xml
replaceInFile feature/pom.xml $PREV_VERSION.qualifier $NEW_VERSION.qualifier 

# update feature/feature.xml
replaceInFile feature/feature.xml $PREV_VERSION.qualifier $NEW_VERSION.qualifier 

# create new updatesite directory
echo "Creating new updatesite directory $NEW_VERSION"
cp -r updatesite/$PREV_VERSION updatesite/$NEW_VERSION

# update updatesite files
echo "Updating new updatesite files"
replaceInFile updatesite/$NEW_VERSION/pom.xml $PREV_VERSION.qualifier $NEW_VERSION.qualifier
replaceInFile updatesite/$NEW_VERSION/pom.xml org.aludratest.eclipse.vde.site.version$PREV_VERSION_SHORT org.aludratest.eclipse.vde.site.version$NEW_VERSION_SHORT
replaceInFile updatesite/$NEW_VERSION/pom.xml "AludraTest VDE $PREV_VERSION Update Site" "AludraTest VDE $NEW_VERSION Update Site" 
replaceInFile updatesite/$NEW_VERSION/category.xml $PREV_VERSION $NEW_VERSION 

replaceInFile updatesite/pom.xml $PREV_VERSION.qualifier $NEW_VERSION.qualifier

# insert new resource XML in updatesite/pom.xml
echo "Inserting new resource XML in updatesite/pom.xml"
grep -B 500 "insert new folders here" updatesite/pom.xml | sed -n '$!p' > tmp.txt
echo "								<resource>" >> tmp.txt 
echo "									<targetPath>$NEW_VERSION</targetPath>" >> tmp.txt
echo "									<directory>$NEW_VERSION/target/repository</directory>" >> tmp.txt
echo "									<filtering>false</filtering>" >> tmp.txt
echo "								</resource>" >> tmp.txt 
echo "								<!-- insert new folders here -->" >> tmp.txt
grep -A 500 "insert new folders here" updatesite/pom.xml | tail -n "+2" >> tmp.txt
mv tmp.txt updatesite/pom.xml

# update compositeArtifacts.xml and compositeContent.xml
echo "Extending composite Repository XML files"
grep -B 500 "<child location='$PREV_VERSION'/>" updatesite/compositeContent.xml > tmp.txt
echo "        <child location='$NEW_VERSION'/>" >> tmp.txt
grep -A 500 "<child location='$PREV_VERSION'/>" updatesite/compositeContent.xml | tail -n "+2" >> tmp.txt
mv tmp.txt updatesite/compositeContent.xml
grep -B 500 "<child location='$PREV_VERSION'/>" updatesite/compositeArtifacts.xml > tmp.txt
echo "        <child location='$NEW_VERSION'/>" >> tmp.txt
grep -A 500 "<child location='$PREV_VERSION'/>" updatesite/compositeArtifacts.xml | tail -n "+2" >> tmp.txt
mv tmp.txt updatesite/compositeArtifacts.xml

# update child counts in both files
PREV_CHILDCOUNT=$(egrep "children size='([0-9]+)'" updatesite/compositeContent.xml | sed s/[^0-9]//g)
NEW_CHILDCOUNT=$(expr $PREV_CHILDCOUNT + 1)
echo "Updating child count to $NEW_CHILDCOUNT in XML files"
replaceInFile updatesite/compositeContent.xml "children size='$PREV_CHILDCOUNT'" "children size='$NEW_CHILDCOUNT'"
replaceInFile updatesite/compositeArtifacts.xml "children size='$PREV_CHILDCOUNT'" "children size='$NEW_CHILDCOUNT'"

echo "Done."
