# git-reverse.sh
Bash 4.x script to reverse a git repo.

More info on the Bit-Booster blog: [Doing Git Wrong](https://bit-booster.com/doing-git-wrong/2017/03/30/howto-reverse-a-git-repo/)


## Compatibility
Usually fails on Mac OS X (Bash 3.x).  Tested on Ubuntu 16.04.  YMMV.

## License
BSD-2

## Why?
The primary usecase for this script is to repair repositories
previously corrupted by this script.

## What Does It Do?
If your repo looks like [this](http://vm.bit-booster.com/bitbucket/plugins/servlet/bb_net/projects/BB/repos/jack/commits):

![Repo with normal chronology](https://bit-booster.com/git-reverse/orig.png)

But you want your repository to look like [this](http://vm.bit-booster.com/bitbucket/plugins/servlet/bb_net/projects/BB/repos/jack_reversed/commits):

![Repo with reversed chronology](https://bit-booster.com/git-reverse/reversed.png)

Then this is your script.

This script can also help teams trying to work with pull requests
from Merlin the Magician or Benjamin Button.

## Example Usage
```bash
git clone --mirror [git-clone-url]
cd [repo.git]
./git-reverse.sh
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
  git push --mirror   

WARNING:
========
Pushing a reversed git repo is a profoundly destructive and confusing operation.

You have a full 'git clone --mirror' backup stored somewhere safe, right?
```

## FAQ

**1. Why are the commits not completely reversed in the 2nd screenshot? E.g., 3 comes before 2.**

   This happens because those commits are on different branches, and the screenshot is backed by "git log --date-order". Since `git-reverse.sh` preserves all meta-data, including author and commit dates, git will present 3 before 2, since this is topologically acceptable, and '3' has a newer datestamp.
   
**2. I tried to use this script to repair a repository corrupted by this script, and it worked, but all my commit-ids changed. Can you fix this?**

   Send me a PR with the fix and I'll happily merge it! I suspect whitespace might be getting messed up in the metadata during the reversal, and so the commit-ids get perturbed.
   After 1 cycle of `git-reverse.sh` the commit-ids stabilize for all subsequent cycles, so there is some hope that this might be fixable.
   
**3. I tried to reverse https://github.com/git/git, but it just hangs after processing about 70,000 commits. Can you fix this?**

   Yes, for some reason the "git commit-tree" call hangs around then. I wonder if maybe git is doing some cleanup behind the scenes, and since all the commits reversed so far are unreachable (they have no references until later in the script), things get into a mess.
   
**4. Those commit graphs above are so pretty!  Where can I get those for my git repositories?**

   They come from [Bit-Booster for Bitbucket Server](https://marketplace.atlassian.com/plugins/com.bit-booster.bb/server/overview), my paid add-on for Bitbucket Server (the on-premises version of Bitbucket that can't handle mercurial repos and is written in Java instead of Python).
