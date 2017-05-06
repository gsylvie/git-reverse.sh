# git-reverse.sh
Reverses a git repo.

## Compatibility
Requires Java 8. Tested on Ubuntu 16.04. YMMV.

## License
BSD-2

## Why?
The primary usecase for this script is to repair repositories previously corrupted by this script.

"git log --follow" and "git blame" only go backwards, so you could use this script to make them go forwards.

## What Does It Do?

It reverses the direction of every edge in your git repository's DAG (directed acyclic commit graph).

In other words, if your repo looks like [this](http://vm.bit-booster.com/bitbucket/plugins/servlet/bb_net/projects/BB/repos/jack/commits):

![Repo with normal chronology](https://bit-booster.com/git-reverse/orig.png)

Run the script to make your repo look like [this](http://vm.bit-booster.com/bitbucket/plugins/servlet/bb_net/projects/BB/repos/jack_reversed/commits):

![Repo with reversed chronology](https://bit-booster.com/git-reverse/reversed.png)

This script can also help teams trying to work with pull requests
from [Merlin the Magician](https://en.wikipedia.org/wiki/Merlin) or [Benjamin Button](https://blog.pinboard.in/2016/10/benjamin_button_reviews_the_new_macbook_pro/).

## Example Usage
```bash
git clone --mirror [git-clone-url]
cd [repo.git]
./git-reverse.sh
git log --all --date-order --graph --decorate
```

## Example Output
```bash
$ ./git-reverse.sh 
756ffd0357b7dfc926ba2fb223c1fe5819fc686f - Reversed 165fe663762a6d6ca0e6bc2a59d38589c8d8db29 - 1 of 8 (12%)
c8483866cf7960fb26f8e5dab1bacfb097c7ab4e - Reversed d59ba45ca49c49945b6425269808b5ddb5d8d414 - 2 of 8 (25%)
0127863a53d594e0108dfb69994627209af4a71c - Reversed 68d6f9be6fbdf4e08a8244f043e634813e7d4948 - 3 of 8 (38%)
ec35739efa3817760e4057bfa425105ecf899b1b - Reversed 6f5b160bc44f5951cc26a74c6b6911e3f6f3cfce - 4 of 8 (50%)
72a6689242c697cf98843ff7c405b0deaa7da536 - Reversed 92a8fc10cc647b4eed47ce0878b6a76897ec6d55 - 5 of 8 (62%)
a89ae14b948aa536f65d2112e82cbf3abef1cc36 - Reversed 7771be1cbe02d0e34baab788a55034f4079a2d81 - 6 of 8 (75%)
1f92a0301852145f8f6b81cb520036f95ea722b0 - Reversed 0bcddcea9ec0252d9099f2c4b740185c0f1da654 - 7 of 8 (88%)
aea136227f86e4d2a12aaa10130f00d538c7fdfd - Reversed eb4de78cec96b8279713e235e9a3fafb419dae9c - 8 of 8 (100%)

*********************************************
| Git repo successfully reversed!!! :-) (-: |
*********************************************
To push the reversed repo:
  rm .git/packed-refs
  rm -rf .git/refs/remotes
  git push --mirror [new-git-clone-url]  

WARNING:
========
Pushing a reversed git repo is a profoundly destructive and confusing operation.

You have a full 'git clone --mirror' backup stored somewhere safe, right?
```

## FAQ

**1. Why are the commits not completely reversed in the 2nd screenshot? E.g., 3 comes before 2.**

   This happens because those commits are on different branches, and the screenshot is backed by "git log --date-order". Since `git-reverse.sh` preserves all meta-data, including author and commit dates, git will present 3 before 2, since this is topologically acceptable, and '3' has a newer datestamp.
   
**2. I tried to use this script to repair a repository corrupted by this script, and it worked, but all my commit-ids changed. Can you fix this?**

   Send me a PR with the fix and I'll happily merge it. I suspect whitespace might be getting messed up in the metadata during the reversal, and so the commit-ids get perturbed. There appears to be a 4-stage cycle using `git-reverse.sh` where commit-ids line up (e.g., commit-id's of the 4th run match the 8th run, 5th run match the 9th, etc) so there is some hope that this might be fixable.
   
   Also, my script inserts the commit message "<empty-commit-message>" in the case where the raw commit object itself contains no commit message (CURSE YOU [git/git](https://github.com/git/git) COMMIT [296fdc53bdd75147](https://github.com/git/git/commit/296fdc53bdd75147) !!!). That also causes that commit-id to change, as well as all subsequent commit-id's.
   
**3. I tried to reverse https://github.com/git/git, but it just hangs after processing 46,173 commits. What's going wrong?**

   FIXED with the new Java version!!! :-D  (But you can invoke the script with "./git-reverse.sh bash" to use the buggy bash logic instead if you prefer this for some reason.)

**4. My repo has a few orphan commits (aka root commits), and they are gone after the reversal. Where are they?**

   They're gone.  They become tip commits after the reversal.  And tip commits without tags or branches pointing at them are not long for this world.
   
   If you happen to know years in advance that you will one day reverse your repo, you can employ the [Always Start With An Empty Commit](https://bit-booster.com/doing-git-wrong/2017/01/02/git-init-empty/) remedy. Or, alternatively, just before invoking the script, throw some tag or branch labels at all your root commits. The script carefully preserves all tags and branches, so this is a good way to save these orphans.
   
   The one exception is your oldest orphan. The script automatically makes your oldest orphan the new "master" branch after the reversal.
   
**5. My repo uses "develop" as its default branch, but the reversed repo seems to be using "master" instead. This makes me very upset. Is there anything I can do about this?**

   After the reversal completes, try `git push origin master:refs/heads/develop`.
   
   Sorry, there's no good way for my script to auto-detect the default branch.

**6. Those commit graphs above are so pretty!  Where can I get those for my git repositories?**

   They come from [Bit-Booster for Bitbucket Server](https://marketplace.atlassian.com/plugins/com.bit-booster.bb/server/overview), my paid add-on for Bitbucket Server (the on-premises version of Bitbucket that can't handle mercurial repos and is written in Java instead of Python).

   Feel free to click around some more on my [demo server](http://vm.bit-booster.com/bitbucket). And feel free to write your own Atlassian add-ons.  It's lots of fun, and somewhat lucrative.
