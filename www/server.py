import logging
import os
import re
from flask import Flask

app = Flask('Dirigible')
app.logger.disabled = True
log = logging.getLogger('werkzeug')
log.disabled = True

VIDEO_EXT = '(\.mp4$)'
ROOT = os.path.dirname(os.path.abspath(__file__)) + "/../www"

@app.route('/', defaults={'path': ''})
@app.route('/<path:path>')
def index(path):
    media = []
    full_path = os.path.join(ROOT, path)
    files = os.listdir(full_path)

    for file in files:
        file_path = os.path.join(full_path, file)

        if not os.path.isfile(file_path) or re.search(VIDEO_EXT, file):
            # directory or video
            media.append('"%s"' % file)
    return '{"data":[%s]}' % ','.join(media)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=80)
