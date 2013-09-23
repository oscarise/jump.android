#!/bin/bash

#based on the update_icons.sh script in jump.ios

set -e

quiltIconUrlBase="http://cdn.quilt.janrain.com/2.2.5/icons/janrain-providers"

# unused, but could provide a list of providers
#quiltProviderJson="http://janrain.quilt.s3.amazonaws.com/2.2.5/providers-vars.json"

if [ $# -ne 1 ]
then
    echo "Usage: `basename $0` <quilt_release_version>"
    echo
    echo "Where <quilt_release_version> is like 3.4.5"
    echo "(Look for quilt release notifications to release-notificiations@janrain.com)"
    echo
    echo This script will update all existing provider image assets. So, to add/update a
    echo new one just touch the appropriate pngs and run the script.
    exit 1
fi

# http://stackoverflow.com/questions/592620/check-if-a-program-exists-from-a-bash-script
if ! hash convert 2>/dev/null; then
    echo "Sorry Bucko, you need ImageMagick installed. (and in your PATH.)"
    echo
    echo "http://www.imagemagick.org/script/binary-releases.php"
    exit 1
fi

tempdir=$(mktemp -dt "$0")

# Unused, but could provide a list of providers
#providerJsonFile=$tempdir/providers.json
#curl $quiltProviderJson 2>/dev/null | gunzip > $providerJsonFile
#cat $providerJsonFile | python -mjson.tool > $providerJsonFile.pretty

# Example icon path:
# ./Janrain/JREngage/Resources/images/icons/icon_facebook_30x30.png

scriptDir=`dirname $0`

colorIconFiles=`find $scriptDir/../Jump -name jr_icon_\*.png -and -not -name \*_bw_\* -and -not -path \*xhdpi\* -and -not -path \*/bin/\*`
colorIconFiles=`echo $colorIconFiles | xargs -n 1 echo`
bwIconFiles=`find $scriptDir/../Jump -name jr_icon_bw_\*.png -and -not -path \*xhdpi\* -and -not -path \*/bin/\*`
bwIconFiles=`echo $bwIconFiles | xargs -n 1 echo`

providers=`echo $colorIconFiles | pcregrep -o1 "jr_icon_(.*?).png"`

for i in $providers 
do
    fullIconUrl=$quiltIconUrlBase/$i.png 
    localIconFile=$tempdir/$i.png
    colorIconFileName=`echo "$colorIconFiles" | grep jr_icon_${i}.png`
    twoExIconFileName=`dirname $colorIconFileName`/../drawable-xhdpi/jr_icon_${i}.png
    echo $twoExIconFileName

    echo Downloading $i.png
    httpResponseCode=$(curl --write-out %{http_code} --silent --output $localIconFile $fullIconUrl)
    if [[ $httpResponseCode != 200 ]] ; then
        echo Error downloading $fullIconUrl
        continue
    fi

    convert $localIconFile -resize 30x30 $colorIconFileName
    convert $localIconFile -resize 60x60 $twoExIconFileName

    # Some providers have "black and white" icons for sharing
    if echo "$bwIconFiles" | grep $i 1>/dev/null ; then
        bwIconFileName=`echo "$bwIconFiles" | grep $i`
        twoExBwIconFileName=`dirname $colorIconFileName`/../drawable-xhdpi/jr_icon_bw_${i}.png
        tempFlatBwIconFileName=$tempdir/flat_bw_$i.png

        # UITabBarItem requires images which use alpha channels for masks (the color channels are ignored.)
        convert $localIconFile -background black -flatten +matte -type Grayscale $tempFlatBwIconFileName
        convert $tempFlatBwIconFileName -alpha copy $tempFlatBwIconFileName

        convert $tempFlatBwIconFileName -resize 30x30 $bwIconFileName
        convert $tempFlatBwIconFileName -resize 60x60 $twoExBwIconFileName
    fi
done

rm -r $tempdir
