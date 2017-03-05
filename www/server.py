import logging
import os
from flask import Flask, send_file

app = Flask('Dirigible')
app.logger.disabled = True
log = logging.getLogger('werkzeug')
log.disabled = True

ROOT = os.path.dirname(os.path.abspath(__file__))

@app.route('/', defaults={'path': ''})
@app.route('/<path:path>')
def index(path):
    if (path.endswith('.jpg')):
        return send_file(path, mimetype='image/jpeg')
    elif (path.endswith('.mp4')):
        return send_file(path, mimetype='video/mp4')
    else:
        media = []
        full_path = os.path.join(ROOT, path)
        files = os.listdir(full_path)

        for file in files:
            file_path = os.path.join(full_path, file)

            if not os.path.isfile(file_path) or file.endswith('.mp4'):
                # directory or video
                media.append('"%s"' % file)
        return '{"data":[%s]}' % ','.join(media)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=80)
