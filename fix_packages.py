import os

root_dir = "services"
old_pkg = "package com.public.admin"
new_pkg = "package com.gov.crypto"
old_import = "import com.public.admin"
new_import = "import com.gov.crypto"

print("Starting package fix...")

for dirpath, dirnames, filenames in os.walk(root_dir):
    for filename in filenames:
        if filename.endswith(".java"):
            filepath = os.path.join(dirpath, filename)
            try:
                with open(filepath, "r") as f:
                    content = f.read()
                
                new_content = content
                if old_pkg in new_content:
                    print(f"Fixing package in {filepath}")
                    new_content = new_content.replace(old_pkg, new_pkg)
                
                if old_import in new_content:
                    print(f"Fixing imports in {filepath}")
                    new_content = new_content.replace(old_import, new_import)
                
                if new_content != content:
                    with open(filepath, "w") as f:
                        f.write(new_content)
            except Exception as e:
                print(f"Error processing {filepath}: {e}")

print("Package fix completed.")
