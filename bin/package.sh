#!/bin/bash

source bin/commons.sh

#
# Make archive for the release
function makeArchive(){
    VERSION=$1
    mkdir choco-${VERSION} || quit "unable to make choco-${VERSION}"
    git checkout ${VERSION} || quit "unable to checkout ${VERSION}"
    mvn clean install -DskipTests || quit "unable to install"

    mv ./target/choco-solver-${VERSION}.jar ./choco-${VERSION} || quit "unable to mv jar"
    mv ./target/choco-solver-${VERSION}-with-dependencies.jar ./choco-${VERSION}|| quit "unable to mv dep"
    mv ./target/choco-solver-${VERSION}-sources.jar ./choco-${VERSION}|| quit "unable to mv src"

    cp ./user_guide.pdf ./choco-${VERSION}/user_guide-${VERSION}.pdf|| quit "unable to cp user guide"
    cp ./README.md ./choco-${VERSION}|| quit "unable to cp README"
    cp ./CHANGES.md ./choco-${VERSION}|| quit "unable to cp CHANGES"
    cp ./LICENSE ./choco-${VERSION}|| quit "unable to cp LICENSE"

    mvn javadoc:aggregate  || quit "unable to javadoc"
    cd target/site/|| quit "unable to cd target/site"
    zip -r apidocs-${VERSION}.zip ./apidocs/*|| quit "unable to zip apidocs"
    mv apidocs-${VERSION}.zip ../../choco-${VERSION}|| quit "unable to mv apidocs"
    cd ../../|| quit "unable to cd ../../"

    zip choco-${VERSION}.zip ./choco-${VERSION}/*|| quit "unable to zip dir"
}
#
# push javadoc onto website
function pushJavadoc(){
    VERSION=$1
    cd choco-${VERSION}|| quit "unable to cd dir"
    git clone https://github.com/chocoteam/chocoteam.github.io.git|| quit "unable to clone website"
    cd chocoteam.github.io/|| quit "unable to go into website"
    git fetch & git pull origin master|| quit "unable to update website"
    unzip ../apidocs-${VERSION}.zip ./apidocs/|| quit "unable to unzip apidocs"
    git commit -a -m "push javadoc of ${VERSION}"|| quit "unable to commit"
    git push origin master || quit "unable to push website"
    cd ..
    rm -r chocoteam.github.io/
}
#
# extract comment from CHANGES.md
function extractReleaseComment(){
    LINES=$(grep -ne "-------------------" CHANGES.md |  cut -d : -f1 | tr "\n" ",")
    IFS=',' read -r -a ARRAY <<< "$LINES"
    FROM=$(expr ${ARRAY[1]} + 1)
    TO=$(expr ${ARRAY[2]} - 2)
    CHANGES=$(cat ./CHANGES.md | sed -n "${FROM},${TO}p" | perl -p -e 's/  /\\t/' | perl -p -e 's/\n/\\n/g')
    echo '{ "tag_name": "choco-4.0.2", "target_commitish": "master","name": "4.0.2","body": '\""$CHANGES"\"' ,"draft": true, "prerelease": false}'
}
#
# create a milestone based on comment ($1), version ($2) and token ($3)
function createRelease(){
    DATA=$1
    VERSION=$2
    TOKEN=$3
    # create release
    curl -i -H 'Authorization: token ${t}' \
            -data $DATA \
            "https://api.github.com/repos/chocoteam/choco-solver/releases"
    # add asset
    curl -i \
         -H "Authorization: token ${TOKEN}" \
         -H "Content-Type: application/zip" \
         --data-binary @choco-${VERSION}.zip \
         "https://uploads.github.com/repos/chocoteam/choco-solver/releases/choco-${VERSION}/assets?name=choco-${VERSION}.zip"
}
# create a milestone based on version ($1) and token ($2)
function createMilestone(){
    VERSION=$1
    TOKEN=$2
    NEXT=$(echo "${VERSION%.*}.$((${VERSION##*.}+1))")
    curl -i -H 'Authorization: token ${t}' \
        -data '{ "title": "${NEXT}"}' \
        "https://api.github.com/repos/chocoteam/choco-solver/milestones"
}

echo "Start release with zip option on"
VERSION=$1

# create archive
makeArchive ${VERSION}

# upload javadoc to webpages
pushJavadoc ${VERSION}

# prepare release comment
#find position of release separator in CHANGES.md, only keep the 2nd and 3rd

# create release
createRelease $(extractReleaseComment) ${VERSION} ${GH_TOKEN}
# create the next milestone
createMilestone ${VERSION} ${GH_TOKEN}

rmdir choco-${VERSION}
git checkout master
