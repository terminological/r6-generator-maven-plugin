# Generated by r6-generator-maven-plugin: remove this line if you want to make manual changes and dont want them to get overwritten
# Workflow derived from https://github.com/r-lib/actions/tree/v2/examples
on:
  push:
    branches: [main, master]
  pull_request:
    branches: [main, master]
## uncomment this to run on a fixed schedule
#  schedule:
#    # * is a special character in YAML so you have to quote this string
#    - cron:  '0 12 * * *'

name: R-CMD-check

jobs:
  R-CMD-check:

    runs-on: ${r"${{ matrix.config.os }}"}

<#if model.getRelativePath()?has_content>
    defaults:
      run:
        working-directory: ${model.getRelativePath()}

</#if>
    continue-on-error: ${r"${{ matrix.config.fail }}"}

    name: ${r"${{ matrix.config.os }} (${{ matrix.config.r }})"}

    strategy:
      fail-fast: false
      matrix:
        java: [ 8, 11 ]
        config:
          - {os: ubuntu-latest, r: 'release', fail: false}
## uncomment this to run on multiple platforms          
#          - {os: windows-latest, r: 'oldrel-1', fail: true}
#          - {os: windows-latest, r: 'release', fail: true}
#          - {os: windows-latest, r: 'devel', http-user-agent: 'release', fail: true}
#          - {os: macOS-latest, r: 'oldrel-1', fail: true}
#          - {os: macOS-latest, r: 'release', fail: true}
#          - {os: macOS-latest, r: 'devel', http-user-agent: 'release', fail: true}
#          - {os: ubuntu-latest, r: 'oldrel-1', fail: true}
#          - {os: ubuntu-latest, r: 'devel', http-user-agent: 'release', fail: true}

    env:
      R_REMOTES_NO_ERRORS_FROM_WARNINGS: true
      RSPM: ${r"${{ matrix.config.rspm }}"}
      GITHUB_PAT: ${r"${{ secrets.GITHUB_TOKEN }}"}

    if: "!contains(github.event.head_commit.message, 'revdepcheck run - No changes to commit')"

    steps:
      - uses: actions/checkout@v2

      - uses: r-lib/actions/setup-pandoc@v2

      - uses: actions/setup-java@v1
        with:
          java-version: ${r"${{ matrix.java }}"}

      - name: Info
        run: "bash -c 'java -version && which java && echo $PATH && echo $JAVA_HOME'"

      - uses: r-lib/actions/setup-r@v2
        with:
          r-version: ${r"${{ matrix.config.r }}"}
          http-user-agent: ${r"${{ matrix.config.http-user-agent }}"}
          use-public-rspm: true

      - uses: r-lib/actions/setup-r-dependencies@v2
        with:
          extra-packages: any::rcmdcheck
          needs: check
<#if model.getRelativePath()?has_content>
          working-directory: ${model.getRelativePath()}
</#if>
      
      - name: Setup R Java support
        if: runner.os != 'Windows'
        run: "echo export PATH=$PATH > reconf.sh; echo export JAVA_HOME=$JAVA_HOME >> reconf.sh; echo R CMD javareconf >> reconf.sh; sudo bash reconf.sh"

      - uses: r-lib/actions/check-r-package@v2
        with:
          upload-snapshots: true
<#if model.getRelativePath()?has_content>
          working-directory: ${model.getRelativePath()}
</#if>