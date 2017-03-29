#!/bin/bash

# git-reverse.sh
# Bash 4.x script to reverse a git repo.
# https://bit-booster.com/doing-git-wrong/2017/03/30/howto-reverse-a-git-repo/
#
# DOES NOT WORK ON MAC OS X (BASH 3.X)
# THIS SCRIPT REQUIRES BASH 4.X ASSOCIATIVE ARRAYS
#
# YMMV - I only tested this on Ubuntu 16.04.

# Copyright (c) 2017, G. Sylvie Davies (sylvie@bit-booster.com)
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
# 
# 1. Redistributions of source code must retain the above copyright notice, this
#    list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright notice,
#    this list of conditions and the following disclaimer in the documentation
#    and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
# ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
# The views and conclusions contained in the software and documentation are those
# of the authors and should not be interpreted as representing official policies,
# either expressed or implied, of bit-booster.com

set -e

unset P
unset C
unset AR
declare -A P # reversed parent commits
declare -A C # old-2-new commit mapping

TOTAL=$(git log --all --pretty="%H" | wc --lines)
COUNT=0

# Store new parent relationships as deferred lookups against the ${C} hash.
# e.g. P[c336a17dadc6ff094fc92687ee0679e9628ec69c] = "-p ${C[a7fa63fe4cb5fb6b745588005e2af8184c650b43]}"
eval "$(git log --all --date-order --pretty='
  export PARENTS="%P";
  for p in $PARENTS; do
    P[$p]="${P[$p]} -p \${C[%H]}";
  done'
)"

# Here's the meat of the operation.
# Essentially: for-each commit "git commit-tree" with the parents reversed.
# Note: extra long commit messages truncated at 30,000 characters to avoid
# command-length limits.
eval "$(git log --all --date-order --pretty='
# %H
readarray AR<<BB_EOM
%aI
%aE
%aN
%cI
%cE
%cN%nBB_EOM
export GIT_AUTHOR_DATE="${AR[0]}"
export GIT_AUTHOR_EMAIL="${AR[1]}"
export GIT_AUTHOR_NAME="${AR[2]}"
export GIT_COMMITTER_DATE="${AR[3]}"
export GIT_COMMITTER_EMAIL="${AR[4]}"
export GIT_COMMITTER_NAME="${AR[5]}"
export GIT_COMMIT_MSG
GIT_COMMIT_MSG="$(head --bytes=30000<<'"'"BB_EOM"'"'%n%B%nBB_EOM%n)"
%nexport GIT_LAST_COMMIT=$(git commit-tree -m "$GIT_COMMIT_MSG" $(eval echo ${P[%H]}) %T)
%nC[%H]=$GIT_LAST_COMMIT
%necho "$GIT_LAST_COMMIT - Reversed $((++COUNT)) of $TOTAL ($((100 * COUNT / TOTAL))%%)"
')"

# Need to get off 'master' branch before we force-update all branches.
# (But don't blow up if in a bare repo).
set +e
git checkout $(git log -1 --pretty=%H)
set -e

eval "$(git for-each-ref refs/heads --shell --format='
  git branch -f %(refname:short) ${C[%(objectname)]}
')"

eval "$(git for-each-ref refs/tags --shell --format='
  git tag -f %(refname:short) ${C[%(objectname)]}
')"

git branch -f master $GIT_LAST_COMMIT

set +e
git checkout master
set -e

echo ""
echo "*********************************************"
echo "| Git repo successfully reversed!!! :-) (-: |"
echo "*********************************************"
echo "To push the reversed repo:"
echo "  rm .git/packed-refs "
echo "  git push --mirror   "
echo ""
echo "WARNING:"
echo "========"
echo "Pushing a reversed git repo is a profoundly destructive and confusing operation."
echo ""
echo "You have a full 'git clone --mirror' backup stored somewhere safe, right?"
echo ""
