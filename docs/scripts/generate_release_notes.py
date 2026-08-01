#!/usr/bin/env python3
import os
import re

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(os.path.dirname(script_dir))
    
    # 1. Try to get version from tag name (if running in GitHub Actions)
    ref_name = os.environ.get('GITHUB_REF_NAME')
    version_name = None
    if ref_name and ref_name.startswith('v'):
        version_name = ref_name[1:]
        print(f"Detected version name from GITHUB_REF_NAME: {version_name}")
        
    # 2. Fall back to build.gradle.kts if not running in action or tag not matching
    if not version_name:
        gradle_path = os.path.join(project_root, 'app', 'build.gradle.kts')
        if os.path.exists(gradle_path):
            with open(gradle_path, 'r', encoding='utf-8') as f:
                gradle_content = f.read()
            version_name_match = re.search(r'versionName\s*=\s*"([^"]+)"', gradle_content)
            if version_name_match:
                version_name = version_name_match.group(1)
                print(f"Parsed version name from build.gradle.kts: {version_name}")

    if not version_name:
        print("Error: Could not determine version name")
        return

    # 3. Locate the existing release notes file
    releasenote_dir = os.path.join(project_root, 'docs', 'releasenote')
    source_path = os.path.join(releasenote_dir, f'release_notes_v{version_name}.md')
    temp_path = os.path.join(releasenote_dir, 'release_notes_temp.md')

    if not os.path.exists(source_path):
        print(f"Error: Release note file {source_path} not found")
        # Write a fallback file so the build/release step doesn't fail
        with open(temp_path, 'w', encoding='utf-8') as f:
            f.write(f"Release notes for version {version_name}")
        return

    # 4. Copy to release_notes_temp.md
    with open(source_path, 'r', encoding='utf-8') as sf:
        content = sf.read()

    with open(temp_path, 'w', encoding='utf-8') as df:
        df.write(content)

    print(f"Successfully copied {source_path} to {temp_path}")

if __name__ == '__main__':
    main()
