# git-reverse.sh
Bash script to reverse a git repo.

# License:
BSD-2

# Why?
The primary usecase for this script is to repair repositories
previously corrupted by this script.

# What Does It Do?
Your repo looks like this:

![Repo with normal chronology](https://bit-booster.com/git-reverse/orig.png)

But you want your repository to look like this:

![Repo with reversed chronology](https://bit-booster.com/git-reverse/reversed.png)

This is your script.

This script can also help teams trying to accept pull requests
from Merlin the Magician or Benjamin Button.

# Example Usage:
```bash
git clone --mirror [git-clone-url]
cd [repo.git]
./git-reverse.sh
```

# Example Output:
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
