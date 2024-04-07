Clone OrganicMaps repository:
https://github.com/organicmaps/organicmaps
(a) $ git clone --recurse-submodules https://github.com/organicmaps/organicmaps.git

Checkout this specific commit ID:
(b) $ git checkout cfd17350b5422209096bdfa39a5e0084d8132653


(c) Copy code from our repo in github.
https://github.com/fan-14/app

You can find organicmaps in app folder. copy organicmaps folder to the repo cloned in the previous step, merge and overwrite the folder.

The reason we only push the source code to our github repo and not the entire OrganicMaps repo is because the repo codebase is big and takes up alot of storage space.

Follow the instructions within the organicmaps repo to compile:
bash ./configure.sh

For the remaining instructions, follow this document and refer to the section "Android app":
https://github.com/organicmaps/organicmaps/blob/master/docs/INSTALL.md
