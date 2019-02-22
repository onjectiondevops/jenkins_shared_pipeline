package org.foo

def checkOutFrom(repo) {
  git url: "${repo}"
}

return this
