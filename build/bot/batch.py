#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import sys

py_dir = sys.executable
script_dir = f"{os.getcwd()}/bot.py"
for i in range(10):
    if os.name == 'nt':
        command = f'start cmd /k "{py_dir} {script_dir} --email=user{i}@example.com --password=password{i}"'
    else:
        command = f'xterm -e "{py_dir} {script_dir} --email=user{i}@example.com --password=password{i}; bash" &'
    os.system(command)