import os
import time
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait, Select
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager

# --- Configuration ---
# Ensure these match your local environment
BASE_URL = "http://localhost:5173"
LOGIN_URL = f"{BASE_URL}/login"
USER_EMAIL = "ravi@customer.com"
USER_PASSWORD = "Customer@123"

def setup_test_assets():
    """Create dummy files for upload in a local directory if they don't exist."""
    assets_dir = os.path.join(os.getcwd(), "test_assets")
    if not os.path.exists(assets_dir):
        os.makedirs(assets_dir)
        print(f"Created directory: {assets_dir}")
    
    files = ["aadhaar.pdf", "license.jpg", "puc.png", "damage.jpg"]
    file_paths = []
    for f in files:
        path = os.path.join(assets_dir, f)
        if not os.path.exists(path):
            with open(path, "w") as fw:
                fw.write("dummy content for " + f)
        file_paths.append(path)
    return file_paths

def run_e2e_test():
    print("\n" + "="*50)
    print("🚀 SmartInsure E2E Selenium Test Starting")
    print("="*50)
    
    # 1. Setup Assets
    file_paths = setup_test_assets()
    print(f"✅ Test assets ready in: {os.getcwd()}/test_assets")

    # 2. Setup WebDriver
    print("🌐 Launching Chrome WebDriver...")
    chrome_options = Options()
    # chrome_options.add_argument("--headless") # Uncomment to run in background
    
    try:
        service = Service(ChromeDriverManager().install())
        driver = webdriver.Chrome(service=service, options=chrome_options)
        wait = WebDriverWait(driver, 20)
    except Exception as e:
        print(f"❌ Failed to start WebDriver: {e}")
        print("💡 Tip: Ensure Chrome is installed and you have an active internet connection for the manager.")
        return

    try:
        # 3. Login
        print(f"🔑 Navigating to {LOGIN_URL}...")
        driver.get(LOGIN_URL)
        
        print(f"👤 Entering credentials: {USER_EMAIL}")
        email_input = wait.until(EC.presence_of_element_located((By.ID, "email")))
        email_input.send_keys(USER_EMAIL)
        
        pass_input = driver.find_element(By.ID, "password")
        pass_input.send_keys(USER_PASSWORD)
        
        submit_btn = driver.find_element(By.CSS_SELECTOR, "button[type='submit']")
        submit_btn.click()
        
        # Wait for dashboard redirect
        wait.until(EC.url_contains("/customer"))
        print("✅ Login successful. Redirected to customer dashboard.")

        # 4. Navigate to New Claim
        print("📝 Opening new claim filing form...")
        driver.get(f"{BASE_URL}/claims/new")
        
        # 5. Fill Step 1: Draft Details
        # Wait for policies to load into the dropdown
        print("⏳ Waiting for policy data...")
        policy_select_elem = wait.until(EC.presence_of_element_located((By.ID, "policy")))
        policy_select = Select(policy_select_elem)
        
        # Polling until policies are populated (more than just the default option)
        wait.until(lambda d: len(policy_select.options) > 1)
        policy_select.select_by_index(1) 
        print(f"✅ Selected Policy: {policy_select.first_selected_option.text}")
        
        desc_input = driver.find_element(By.ID, "desc")
        desc_input.send_keys("Automated test: Minor front-end collision near MG Road. No internal engine damage visible.")
        
        loc_input = driver.find_element(By.ID, "loc")
        loc_input.send_keys("Bangalore, KA")
        
        create_btn = driver.find_element(By.XPATH, "//button[contains(text(), 'Create draft claim')]")
        create_btn.click()
        
        print("✅ Draft claim created successfully.")

        # 6. Step 2: Upload Documents
        # Wait for the file inputs to appear in Step 2
        print("📁 Locating upload fields...")
        inputs = wait.until(EC.presence_of_all_elements_located((By.CSS_SELECTOR, "input[type='file']")))
        
        if len(inputs) < 4:
            raise Exception(f"Expected 4 upload inputs for AI pack, found {len(inputs)}")
        
        print("📤 Uploading: Aadhaar, DL, PUC, and Damage Photo...")
        for i, path in enumerate(file_paths):
            # The inputs are hidden in UI. We make them visible to interact or send keys directly.
            driver.execute_script("arguments[0].style.display = 'block';", inputs[i])
            inputs[i].send_keys(path)
            time.sleep(0.3) 

        upload_btn = driver.find_element(By.XPATH, "//button[contains(text(), 'Upload documents')]")
        upload_btn.click()
        
        print("⏳ Processing uploads...")
        # Give it a moment to finish multi-part uploads
        time.sleep(3) 

        # 7. Step 3: Trigger AI Pipeline
        print("🤖 Triggering AI Verification Pipeline...")
        submit_ai_btn = wait.until(EC.element_to_be_clickable((By.XPATH, "//button[contains(text(), 'Submit for AI review')]")))
        submit_ai_btn.click()

        # 8. Verify Tracking Page & Results
        print("📊 Waiting for AI inference results...")
        # The URL transitions to /claims/CLMXYYYY
        wait.until(EC.url_matches(r".*/claims/CLM.*"))
        
        # Verify that we see some AI metrics
        wait.until(EC.presence_of_element_located((By.XPATH, "//*[contains(text(), 'Severity')]")))
        
        print("="*50)
        print(f"🎉 E2E TEST PASSED!")
        print(f"🔗 View Tracked Claim: {driver.current_url}")
        print("="*50)
        
    except Exception as e:
        print("\n" + "!"*50)
        print(f"❌ TEST FAILED: {str(e)}")
        print("!"*50)
        driver.save_screenshot("selenium_test_failure.png")
        print("📸 Failure screenshot saved: selenium_test_failure.png")
    finally:
        print("\nClosing browser in 5 seconds...")
        time.sleep(5)
        driver.quit()

if __name__ == "__main__":
    run_e2e_test()
