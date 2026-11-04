#!/usr/bin/env python3
"""Real calendar/engine/journal knockout integration and migration audit."""
import sys
from pathlib import Path
from test_v85_long_career import main
ROOT=Path(__file__).resolve().parents[1]
if __name__=='__main__':
    sys.argv=[sys.argv[0],sys.argv[1],str(ROOT/'artifacts/v91-ucl-audit.txt'),'1','1']
    main('pfm.android.UclHarness',[ROOT/'tools/tests/v91/UclHarness.java'])
