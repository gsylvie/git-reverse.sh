# git-reverse.sh
Reverses a git repo. Requires Bash 4 or newer.

## Compatibility
Should fail on Mac OS X (Bash 3).  Tested on Ubuntu 16.04.  YMMV.

## License
BSD-2

## Why?
The primary usecase for this script is to repair repositories previously corrupted by this script.

I considered using this script to prank my colleagues for April Fool's, but I chickened out.

I actually own a real live chicken (a white silkie), and she also chickened out.

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
be0923ece8f73281e5e54906c29debb852894b92 - Reversed 1 of 360 (0%)
061bca59b29e75becbde66d2e510fc2b4059ccb2 - Reversed 2 of 360 (0%)
6a19039e05e3a0186187d0a6943634e8499b5a65 - Reversed 3 of 360 (0%)
7afe25e8caf93eb7107471a1bd078d4adc3f6999 - Reversed 4 of 360 (1%)
8b556f6fe97c5357c2328467a5ba01b77931ff82 - Reversed 5 of 360 (1%)
71fd0883975154f48059ca929db8ccb659c5049a - Reversed 6 of 360 (1%)
[etc...]
Switched to branch 'master'
Your branch and 'origin/master' have diverged,
and have 199 and 159 different commits each, respectively.
  (use "git pull" to merge the remote branch into yours)

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

   Send me a PR with the fix and I'll happily merge it. I suspect whitespace might be getting messed up in the metadata during the reversal, and so the commit-ids get perturbed. After 1 cycle of `git-reverse.sh` the commit-ids stabilize for all subsequent cycles, so there is some hope that this might be fixable.
   
   Also, my script ignores anything in a commit message past the 30,000th character. That also causes that commit-id to change, as well as all subsequent commit-id's.  I'd welcome a PR that fixes that. We need to use a temporary file to hold the commit message to get around maximum command-length limitations.
   
**3. I tried to reverse https://github.com/git/git, but it just hangs after processing 76,173 commits. What's going wrong?**

   For some reason the "git commit-tree" invocation hangs at that point.  I have no idea why.  Same for Atlassian's aui repo (after commit 14,378). But I was able to reverse [node.git](http://vm.bit-booster.com/bitbucket/projects/BB/repos/node_reversed/commits).  I've only tested with Git 2.7.4.  Maybe a different git version would fix this. But I really have no idea.

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
