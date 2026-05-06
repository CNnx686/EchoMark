import os
from flask import Flask, request, jsonify, send_from_directory

app = Flask(__name__)
# Use a local directory relative to this script for storage
BASE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "static")

os.makedirs(BASE_DIR, exist_ok=True)


def is_safe_path(filepath):
    # Resolve the absolute path of the requested file
    full_path = os.path.abspath(os.path.join(BASE_DIR, filepath))
    # Ensure the file is within the BASE_DIR
    return full_path.startswith(BASE_DIR)

@app.route('/upload/<path:filepath>', methods=['PUT'])
def upload_file(filepath):
    if not is_safe_path(filepath):
        return jsonify({"error": "Invalid path"}), 400

    full_path = os.path.join(BASE_DIR, filepath)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)

    with open(full_path, 'wb') as f:
        f.write(request.get_data())

    return jsonify({"status": "uploaded", "path": filepath}), 201


@app.route('/files/<path:filepath>', methods=['GET'])
def download_file(filepath):
    if not is_safe_path(filepath):
        return jsonify({"error": "Invalid path"}), 400

    try:
        return send_from_directory(BASE_DIR, filepath)
    except FileNotFoundError:
        return jsonify({"error": "File not found"}), 404


@app.route('/delete/<path:filepath>', methods=['DELETE'])
def delete_file(filepath):
    if not is_safe_path(filepath):
        return jsonify({"error": "Invalid path"}), 400

    full_path = os.path.join(BASE_DIR, filepath)
    if os.path.exists(full_path) and os.path.isfile(full_path):
        os.remove(full_path)
        # Try to remove empty parent directories
        parent = os.path.dirname(full_path)
        while parent != BASE_DIR:
            try:
                if not os.listdir(parent):
                    os.rmdir(parent)
                else:
                    break
            except OSError:
                break
            parent = os.path.dirname(parent)
        return jsonify({"status": "deleted"}), 200
    else:
        return jsonify({"error": "File not found"}), 404


if __name__ == '__main__':
    print(f"Starting local resource server on port 5000...")
    print(f"Storage directory: {BASE_DIR}")
    app.run(host='0.0.0.0', port=5000)
