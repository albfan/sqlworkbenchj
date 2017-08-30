[![Build Status](https://travis-ci.org/albfan/sqlworkbenchj.svg?branch=master)](https://travis-ci.org/albfan/sqlworkbenchj)

# SQL Workbench/J

A fork from this fantastic SQL workbench

## git-svn setup

SQL Workbench/J doesn't follow a standard layout so this is the config you need to clone and be up to date with svn

```
[svn-remote "svn"]
        url = http://sqlworkbench.mgm-tp.com/sqlworkbench
        fetch = trunk/sqlworkbench:refs/remotes/origin/trunk
        branches = branches/*:refs/remotes/origin/*
        tags = tags/{build51,BUILD_54,BUILD_55,BUILD_57,BUILD_58,BUILD_61,BUILD_62,BUILD_63,BUILD_66,BUILD_67,BUILD_68,BUILD_69,BUILD_70,BUILD_72,BUILD_73,build_74,build_75,build_77,build_78,build_80,build_84,build_91,build_92,build_93,build_94,build_95,build_97,build_98,build_99,build_101}/sqlworkbench:refs/remotes/origin/tags/*
        tags = tags/{build_103,build_105,build_107,build_109,build_110,build_115,build_117,build_11(,build_118,build_119,build_120,build_121,build_122}:refs/remotes/origin/tags/*
```

You need to fetch from last revision showed on git-svn-id

    git svn fetch -r7491:HEAD

## Motivation

This is a powerful and useful project, for a regular basis work on SQL. Although it has a wide community and an active vcs, svn and ant seems a little odd this days and I feel not comfortable with that workflow. To improve or modify it I prefer git and maven management.

Anyway it will be a target for this fork to contribute upstream repo, so git-svn will be the base for development.
 
## Install

As this project is mavenized and some SQL engines are not on maven you need to install it yourself

Execute [install_no_maven_dependencies.sh](https://github.com/albfan/sqlworkbenchj/blob/master/libs/junit/install_no_maven_dependencies.sh) for that

## Info

- website: http://www.sql-workbench.net/
- manual: http://www.sql-workbench.net/manual/
